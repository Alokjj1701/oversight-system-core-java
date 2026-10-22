package com.oversight.issuance.event;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Status;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened to an issuance. eventId is unique per event and is what consumers
 * use to ignore duplicates. issuanceId becomes the Kafka partition key in Phase 4.
 */
public record IssuanceEvent(String eventId, String issuanceId, Status from, Status to,
                            String actorId, Instant occurredAt) {

    public static IssuanceEvent of(Issuance issuance, Status from, String actorId) {
        return new IssuanceEvent(UUID.randomUUID().toString(), issuance.getId(), from,
                issuance.getStatus(), actorId, Instant.now());
    }
}
