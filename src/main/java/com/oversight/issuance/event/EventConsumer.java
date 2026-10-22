package com.oversight.issuance.event;

/** A downstream system that reacts to events. In Phase 5 each one is its own service. */
public interface EventConsumer {

    void handle(IssuanceEvent event);
}
