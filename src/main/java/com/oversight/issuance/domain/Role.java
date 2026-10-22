package com.oversight.issuance.domain;

/** What a user is allowed to do. In Phase 3 these come from JWT / AD groups. */
public enum Role {
    SUBMITTER,
    APPROVER_L1,
    APPROVER_L2,
    PUBLISHER
}
