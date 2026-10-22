package com.oversight.issuance.domain;

import java.time.Instant;

/** One line of the audit trail: who moved the issuance from what to what, and when. */
public record StatusChange(Status from, Status to, String actorId, Instant at, String comment) {
}
