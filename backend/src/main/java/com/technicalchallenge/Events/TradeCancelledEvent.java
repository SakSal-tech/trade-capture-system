package com.technicalchallenge.Events;

import java.time.Instant;

/**
 * Domain event published when a trade is cancelled.
 *
 * Purpose: notify internal listeners about trade cancellations so they can
 * persist notifications, update real-time UIs, or trigger downstream
 * processing. This is a simple immutable DTO for event publishing.
 */
public final class TradeCancelledEvent {

    /** Business trade identifier (string form) */
    private final String tradeId;

    /** Optional numeric database id (PK) */
    private final long tradeDbId;

    /** Username of the actor who cancelled the trade */
    private final String cancelledBy;

    /** When the cancellation occurred */
    private final Instant timestamp;

    /** Optional cancellation reason (may be null) */
    private final String reason;

    public TradeCancelledEvent(String tradeId, long tradeDbId, String cancelledBy, Instant timestamp, String reason) {
        this.tradeId = tradeId;
        this.tradeDbId = tradeDbId;
        this.cancelledBy = cancelledBy;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public String getTradeId() {
        return tradeId;
    }

    public long getTradeDbId() {
        return tradeDbId;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }

}