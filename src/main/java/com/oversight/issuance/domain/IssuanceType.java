package com.oversight.issuance.domain;

/** The 8 workflows. In Phase 5 some of these may become their own services. */
public enum IssuanceType {
    NEW_ISSUANCE,
    TAP,
    BUYBACK,
    MATURITY_EXTENSION,
    CALL,
    COUPON_CHANGE,
    CANCELLATION,
    DOCUMENT_SIGN_OFF
}
