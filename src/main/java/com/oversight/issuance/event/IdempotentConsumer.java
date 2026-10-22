package com.oversight.issuance.event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a consumer so the same event is processed at most once.
 *
 * The outbox relay is at-least-once: if it crashes after delivering but before marking an
 * entry sent, it delivers that event again. Remembering processed eventIds makes the
 * duplicate harmless. In Phase 4 the set becomes a processed_event table written in the
 * same transaction as the consumer's own update.
 */
public abstract class IdempotentConsumer implements EventConsumer {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public final void handle(IssuanceEvent event) {
        if (!processed.add(event.eventId())) {   // add() returns false if already present: atomic check-and-mark
            return;
        }
        try {
            process(event);
        } catch (RuntimeException e) {
            processed.remove(event.eventId());   // failed: allow the redelivery to try again
            throw e;
        }
    }

    protected abstract void process(IssuanceEvent event);
}
