package com.oversight.issuance.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(Status.DRAFT.canMoveTo(Status.SUBMITTED));
        assertTrue(Status.SUBMITTED.canMoveTo(Status.L1_APPROVED));
        assertTrue(Status.L1_APPROVED.canMoveTo(Status.L2_APPROVED));
        assertTrue(Status.L2_APPROVED.canMoveTo(Status.PUBLISHED));
    }

    @Test
    void cannotSkipApprovals() {
        assertFalse(Status.DRAFT.canMoveTo(Status.PUBLISHED));
        assertFalse(Status.SUBMITTED.canMoveTo(Status.L2_APPROVED));
        assertFalse(Status.SUBMITTED.canMoveTo(Status.PUBLISHED));
    }

    @Test
    void finalStatesAreTerminal() {
        assertTrue(Status.PUBLISHED.isTerminal());
        assertTrue(Status.REJECTED.isTerminal());
        assertFalse(Status.REJECTED.canMoveTo(Status.SUBMITTED));
    }
}
