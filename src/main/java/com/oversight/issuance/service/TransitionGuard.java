package com.oversight.issuance.service;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Status;
import com.oversight.issuance.domain.User;

/**
 * One business rule about who may move an issuance. Adding a rule = adding a class
 * (Open/Closed principle). In Phase 2 each guard is a Spring bean and the service gets
 * all of them injected as a List.
 */
public interface TransitionGuard {

    /** Throws OversightException.Forbidden if {@code actor} may not move {@code issuance} to {@code next}. */
    void check(Issuance issuance, Status next, User actor);
}
