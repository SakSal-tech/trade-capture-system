package com.technicalchallenge;

import java.math.BigDecimal;
import java.time.Instant;

public final class RiskExposureChangedEvent {
    private final String tradeId;
    private final long tradeDbId;
    private final BigDecimal oldExposure;
    private final BigDecimal newExposure;
    private final Instant timestamp;

    public String getTradeId() {
        return tradeId;
    }

    public long getTradeDbId() {
        return tradeDbId;
    }

    public BigDecimal getOldExposure() {
        return oldExposure;
    }

    public BigDecimal getNewExposure() {
        return newExposure;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public RiskExposureChangedEvent(String tradeId, long tradeDbId, BigDecimal oldExposure, BigDecimal newExposure,
            Instant timestamp) {
        this.tradeId = tradeId;
        this.tradeDbId = tradeDbId;
        this.oldExposure = oldExposure;
        this.newExposure = newExposure;
        this.timestamp = timestamp;
    }

}