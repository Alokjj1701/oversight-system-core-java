package com.oversight.issuance.service;

import com.oversight.issuance.domain.IssuanceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

/** Input for creating an issuance. In Phase 2 this is the REST request body, with @Valid annotations. */
public record CreateIssuanceRequest(IssuanceType type, String issuer, String isin, BigDecimal amount,
                                    Currency currency, LocalDate maturityDate) {
}
