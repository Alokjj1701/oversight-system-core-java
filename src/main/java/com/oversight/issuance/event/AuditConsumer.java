package com.oversight.issuance.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Append-only record of every event, for auditors. Phase 5: audit-service. */
public class AuditConsumer extends IdempotentConsumer {

    private final List<IssuanceEvent> log = new CopyOnWriteArrayList<>();

    @Override
    protected void process(IssuanceEvent event) {
        log.add(event);
    }

    public List<IssuanceEvent> entries() {
        return List.copyOf(log);
    }
}
