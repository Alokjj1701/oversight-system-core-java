package com.oversight.issuance.event;

import java.util.List;

/**
 * Moves events from the outbox to every consumer, then marks them sent.
 *
 * Phase 4 replaces "call each consumer" with "publish to a Kafka topic keyed by issuanceId",
 * and runs this on a schedule. The crash window between delivering and markSent() is why
 * delivery is at-least-once and consumers must be idempotent.
 */
public class OutboxRelay {

    private final Outbox outbox;
    private final List<EventConsumer> consumers;

    public OutboxRelay(Outbox outbox, List<EventConsumer> consumers) {
        this.outbox = outbox;
        this.consumers = List.copyOf(consumers);
    }

    /** Delivers up to {@code batchSize} pending events. Returns how many were sent. */
    public int relayOnce(int batchSize) {
        int sent = 0;
        for (Outbox.Entry entry : outbox.unsent(batchSize)) {
            consumers.forEach(c -> c.handle(entry.event()));
            outbox.markSent(entry);
            sent++;
        }
        return sent;
    }
}
