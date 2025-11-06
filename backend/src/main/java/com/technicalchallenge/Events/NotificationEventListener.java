package com.technicalchallenge.Events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Simple event listener that demonstrates handling of domain events.
 *
 * Current behaviour: log received events. Later this component can be
 * extended to persist Notifications, push SSE messages, or enqueue work for
 * asynchronous processing.
 * // TODO: Future improvement after deadline, persist Notification entity
 * and/or publish to SSE clients
 * 
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    // Listen for settlement instruction changes and handle them.
    // This method is invoked by Spring when a SettlementInstructionsUpdatedEvent is
    // published.
    @EventListener
    public void onSettlementUpdated(SettlementInstructionsUpdatedEvent ev) {
        // Log a concise summary for observability (trade id, db id, actor and details)
        log.info("SettlementInstructionsUpdatedEvent received for tradeId={} dbId={} by={} details={}",
                // Get Business-facing trade id (string) used by UI/listeners
                ev.getTradeId(),
                // Get Numeric DB id (may be 0 or null-handled by publisher) for quick DB
                // lookups
                ev.getTradeDbId(),
                // Get Username who triggered the change
                ev.getChangedBy(),
                // Minimal payload (old/new) that listeners can use to build notifications
                ev.getDetails());
    }

    // Listen for risk exposure updates and handle them (same pattern as settlement
    // updates).
    // Invoked by Spring when a RiskExposureChangedEvent is published.
    @EventListener
    public void onRiskExposureChanged(RiskExposureChangedEvent ev) {
        // Log key fields so operators and CI can trace exposures (trade id, db id,
        // actor, old/new values)
        log.info("RiskExposureChangedEvent received for tradeId={} dbId={} by={} old={} new={}",
                // Business-facing trade id
                ev.getTradeId(),
                // Numeric DB id used for internal lookups
                ev.getTradeDbId(),
                // Username who made the change
                ev.getChangedBy(),
                // previous exposure value
                ev.getOldExposure(),
                // new exposure value
                ev.getNewExposure());
    }

}
