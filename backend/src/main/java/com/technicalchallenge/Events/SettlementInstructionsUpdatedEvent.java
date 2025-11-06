package com.technicalchallenge.Events;

import java.time.Instant;
import java.util.Map;

/**
 * Eevent DTO published when settlement instructions are
 * created/updated/deleted.
 * Purpose: carry lightweight audit/context information to listeners (for
 * example to persist notifications, to send SSE messages, or to record an
 * external audit). This class is intentionally immutable(no setters which makes
 * it safe to publish and share across threads (no locking needed), simplifies
 * reasoning for listeners, and prevents accidental mutation bugs).
 */
public final class SettlementInstructionsUpdatedEvent {

    private final String tradeId;

    /** Optional numeric database id for convenience when listeners need a PK */
    private final long tradeDbId;

    /** The username that triggered the change (derived from SecurityContext) */
    private final String changedBy;

    /** Event creation time (UTC) */
    private final Instant timestamp;

    /** Optional additional details (to keep old and new settlements) */
    private final Map<String, Object> details;

    /**
     * Create a new event instance.
     *
     * @param tradeId   business id of the trade
     * @param tradeDbId optional DB id (0 if unknown)
     * @param changedBy actor who made the change
     * @param timestamp when the change happened
     * @param details   optional extra metadata (may be null)
     */
    public SettlementInstructionsUpdatedEvent(String tradeId, long tradeDbId, String changedBy, Instant timestamp,
            Map<String, Object> details) {
        this.tradeId = tradeId;
        this.tradeDbId = tradeDbId;
        this.changedBy = changedBy;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getTradeId() {
        return tradeId;
    }

    public long getTradeDbId() {
        return tradeDbId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

}
