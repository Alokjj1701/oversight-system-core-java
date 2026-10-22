package com.oversight.issuance.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of an issuance. The allowed moves are data (a map), not if/else chains,
 * so adding a level later means changing one table.
 *
 * DRAFT -> SUBMITTED -> L1_APPROVED -> L2_APPROVED -> PUBLISHED
 *                 \-> REJECTED    \-> REJECTED
 */
public enum Status {
    DRAFT, SUBMITTED, L1_APPROVED, L2_APPROVED, PUBLISHED, REJECTED;

    private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Status.class);

    static {
        ALLOWED.put(DRAFT, EnumSet.of(SUBMITTED));
        ALLOWED.put(SUBMITTED, EnumSet.of(L1_APPROVED, REJECTED));
        ALLOWED.put(L1_APPROVED, EnumSet.of(L2_APPROVED, REJECTED));
        ALLOWED.put(L2_APPROVED, EnumSet.of(PUBLISHED));
        ALLOWED.put(PUBLISHED, EnumSet.noneOf(Status.class));
        ALLOWED.put(REJECTED, EnumSet.noneOf(Status.class));
    }

    public boolean canMoveTo(Status next) {
        return ALLOWED.get(this).contains(next);
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }
}
