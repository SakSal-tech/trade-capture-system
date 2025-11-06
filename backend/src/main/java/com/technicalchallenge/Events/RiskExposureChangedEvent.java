package com.technicalchallenge.Events;

import java.time.Instant;

/**
 * Domain event published when a trade's risk exposure changes.
 *
 * Purpose: allow listeners to persist notifications, update realtime UIs, or
 * trigger downstream risk recalculations. Immutable DTO.
 */
public final class RiskExposureChangedEvent {

    private final String tradeId;
    private final long tradeDbId;
    private final String changedBy;
    private final Instant timestamp;
    private final Double oldExposure;
    private final Double newExposure;

    public RiskExposureChangedEvent(String tradeId, long tradeDbId, String changedBy, Instant timestamp,
            Double oldExposure, Double newExposure) {
        this.tradeId = tradeId;
        this.tradeDbId = tradeDbId;
        this.changedBy = changedBy;
        this.timestamp = timestamp;
        this.oldExposure = oldExposure;
        this.newExposure = newExposure;
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

    public Double getOldExposure() {
        return oldExposure;
    }

    public Double getNewExposure() {
        return newExposure;
    }

}
