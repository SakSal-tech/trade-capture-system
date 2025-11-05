# Step 3 Production notifications: implementation notes, rationale and alternatives

## Overview and objectives

Objective

- Provide near‑real‑time notifications to the operations UI (and any other interested listeners) whenever settlement instructions change for a trade.
- Preserve existing persistence and audit semantics (the service must still validate, persist and audit every change).
- Deliver a small, testable and reversible implementation that can later be migrated to a durable broker (Kafka, RabbitMQ) if operational needs require.

Constraints I observed in the codebase

- `AdditionalInfoService.upOrInsertTradeSettlementInstructions(...)` is the canonical, transactional method that validates input, writes the AdditionalInfo row and writes the audit trail. Because this method encloses the full business transaction, it is the correct place to publish notification events.
- Controllers are intentionally thin and delegate to services. To keep controllers thin I published events from the service layer rather than from controllers.

Chosen high‑level architecture

- A lightweight Server‑Sent Events (SSE) approach: an in‑process emitter manager plus a compact event class for settlement updates.
- The service publishes an event only after audit write completes so subscribers receive only persisted, auditable changes.
- A subscription endpoint (SSE) allows frontends or admin consoles to subscribe to either a specific `tradeId` or a wildcard feed.

Rationale short

- SSE is simple, firewall friendly and fits a unidirectional server→client use case (operations UIs mainly consume events).
- Emitting from the service ensures correctness subscribers only receive committed and audited changes.

---

## Enhancement 1 Publishing settlement-updated events from the service

What I implemented

- Publication occurs from `AdditionalInfoService.upOrInsertTradeSettlementInstructions(Long tradeId, String settlementText, String changedBy)` immediately after the audit record is saved.
- The published payload contains: `tradeId`, `changedBy` (authenticated principal derived in the service), `changedAt` timestamp, a short `changeSummary` (created/updated + old/new values) and optionally the `AdditionalInfoDTO` with the settlement text.

Why publish from the service

- The service method is transactional and performs validation, db upsert and audit save. Publishing only after audit save guarantees subscribers see authoritative state and prevents events for failed saves.
- Keeping event emission in the service respects separation of concerns: controllers handle HTTP/REST concerns, services own business rules and side effects.

Programming techniques used

- Transactional safety: emission happens after the `additionalInfoAuditRepository.save(audit)` call that writes the audit record. This ordering ensures durability (from the application perspective) before notifying subscribers.
- Defensive checks: emit only when there is a meaningful change (oldValue != newValue). This reduces noisy events on idempotent updates.
- Use of authenticated principal: the service reads the username from `SecurityContextHolder` (the code already derives `authUser`) and uses that value in the event payload to prevent spoofed actor values.

Alternatives considered (and why I deferred them)

- Directly publishing to an external broker (Kafka/RabbitMQ) from the service.
  - Pros: Durable, scalable, decouples producers and consumers.
  - Cons: Requires infra, extra config, security hardening. For a first incremental rollout this is heavy and increases operational burden.
  - Decision: Defer to a follow‑up step after we validate UX and volume.
- Using Spring ApplicationEventPublisher to fire an application event and then a listener sends to a broker.
  - Pros: decouples publisher/listener responsibilities and simplifies unit testing.
  - Cons: still requires a downstream transport implementation and slightly more moving parts.
  - Decision: acceptable alternative; easy to switch to later.
- Publishing from the controller.
  - Rejected because controllers may publish before transaction commit and it duplicates logic between booking and amendment paths.

Testing notes

- Unit test: mock an emitter manager and assert the publish method is invoked with the expected `tradeId`, `changedBy` and a timestamp when `upOrInsertTradeSettlementInstructions` runs.
- Integration test: use MockMvc to issue a PUT and assert the in‑test SSE client receives the expected event (synchronous verification within test timeout).

Code references

- `AdditionalInfoService.upOrInsertTradeSettlementInstructions(...)` emission point (after `additionalInfoAuditRepository.save(audit)` and `additionalInfoRepository.save(...)`).

---

## Enhancement 2 SSE subscription endpoint and emitter manager

What I implemented conceptually

- A subscription endpoint that returns an `SseEmitter` and registers it in an in‑memory manager keyed by `tradeId` (or a wildcard key for all trades).
- The manager holds a thread-safe map: `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>` (or similar), with cleanup handlers for completion, error and timeout.

Why SSE

- Browser friendly: `EventSource` is supported natively and handles reconnection automatically.
- Simpler to implement and operate than WebSockets for a unidirectional use case.
- Less operational friction for early rollout works through corporate proxies and standard ports.

Programming techniques used

- Concurrency: use thread‑safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) to avoid synchronization bugs when multiple threads register/deregister emitters.
- Emitter lifecycle handling: attach `onCompletion`, `onTimeout` and `onError` callbacks to remove dead emitters from the manager avoids memory leaks.
- Heartbeats: send occasional keep‑alive comments or ping events so intermediary proxies don’t silently drop idle connections.
- Security: protect the endpoint with `@PreAuthorize` and perform the same ownership and privilege checks used by `AdditionalInfoService.getSettlementInstructionsByTradeId(...)` so one user cannot subscribe to another user’s private trade unless authorised.

Alternatives considered and why I deferred them

- WebSocket server (Spring WebSocket + STOMP)
  - Pros: bidirectional, better for interactive editors or chatty UIs.
  - Cons: more complex server and load‑balancer setup; requires session affinity or a broker for multi‑instance scaling. Not strictly necessary for read‑only, event‑streaming for operations.
  - Decision: keep WebSocket as a later enhancement only if we need bidirectional or very low latency updates.
- Broker + fan‑out service
  - Pros: durable and horizontally scalable; consumer decoupling.
  - Cons: external infra required and more work for on‑boarding.
  - Decision: recommended when scaling to multiple instances or many subscribers.

Scaling limitations

- In‑memory SSE does not scale across multiple application instances and offers no durable delivery. For multi‑instance production, I recommended introducing a message broker (Kafka) and a separate fan‑out service that consumes broker messages and pushes to connected SSE/WebSocket clients.

Testing notes

- Manager unit tests: register emitter, send event, ensure emitter receives data; ensure cleanup on timeout/completion.
- MockMvc integration: open an SSE connection in test, call the PUT API, assert the SSE stream receives the `settlement‑updated` event payload.

Code references

- `TradeSettlementController` the natural place to expose the subscription endpoint beside existing settlement GET/PUT endpoints so endpoints remain grouped by concern.

---

## Enhancement 3 Client integration, ops notes and migration path

Client integration guidance I provided

- Use native `EventSource` with a small wrapper to handle reconnects with exponential backoff.
- On subscription, optionally send an initial event that includes the current settlement instructions so the UI need not issue an immediate GET unless it needs the full canonical record.
- Keep event payloads small. If settlement text is large, include a short summary and let the client call `GET /api/trades/{id}/settlement-instructions` to fetch full details.

Operational and security considerations

- Authentication: subscription endpoint must be protected by same auth and ownership checks as read endpoints.
- Monitoring: add metrics for active emitter counts, events published and publish errors.
- Reconciliation: because SSE is best‑effort, clients should re‑poll the GET endpoint on reconnect or periodically to reconcile missed updates.

Migration path to durable architecture

1. Replace in‑memory emitter manager with a publisher that writes `SettlementUpdated` messages to Kafka. The message will contain the same payload used by SSE but as a durable record.
2. Implement a small fan‑out service that consumes Kafka and pushes to connected SSE/WebSocket clients. This allows horizontal scaling and multi‑instance application topology.
3. Optionally, adapt the service publisher to use Spring `ApplicationEventPublisher` this decouples the event from the transport and simplifies unit testing.

Testing & verification checklist (what I ran or suggested to run)

- Unit tests (fast): mock emitter manager and assert `publish` invoked when `upOrInsertTradeSettlementInstructions` runs.
- Integration tests (MockMvc + SSE): open SSE connection, execute a `PUT` to the settlement endpoint, assert receipt of `settlement-updated` event.
- Security tests: ensure unauthorised users cannot subscribe to trades they do not own.

Why I did it this way full justification

- Correctness and compliance: audit-first emission ensures subscribers see only persisted and auditable changes this was non-negotiable for operations and compliance.
- Low friction rollout: SSE requires no additional infra and is easy to test and demo to stakeholders. It buys immediate operational value while keeping the long‑term design open to durable options.
- Future‑proof: by publishing from the service layer (not the controller) and keeping event publication abstract (via a manager or an event publisher), we made the implementation easy to refactor to a brokered, durable architecture later.

Known limitations and recommendations

- Limitations
  - Not durable: in‑memory SSE will lose events on instance restart or crash.
  - Not horizontally scalable without a broker/fanout layer.
- Recommendations
  - Add metrics and monitoring for emitter counts and event errors.
  - Migrate to Kafka + fan‑out service if the number of subscribers or instances grows.
  - Consider coalescing frequent updates (debounce) for chatty trades.

---

## Exact classes and methods I referenced while writing this document

- `AdditionalInfoService.upOrInsertTradeSettlementInstructions(Long tradeId, String settlementText, String changedBy)` central upsert + audit method and emission point.
- `AdditionalInfoService.saveOrUpdateSettlementInstructions(...)`, `createAdditionalInfo(...)`, `updateAdditionalInfo(...)` other service helpers used during trade booking and amendments.
- `TradeSettlementController.getSettlementInstructions(...)` and `updateSettlementInstructions(...)` endpoints that contain the read/update contract and were the natural place to add subscription endpoint in the same controller.
- `TradeController.createTrade(...)` trade booking logic saves initial settlement instructions through `additionalInfoService.createAdditionalInfo(...)`. Because booking and amendment feed into the same service methods, publishing at the service layer covers both paths.
