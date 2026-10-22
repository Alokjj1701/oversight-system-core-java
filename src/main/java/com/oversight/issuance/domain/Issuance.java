package com.oversight.issuance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The aggregate everything revolves around. Deliberately a normal mutable class, not a record:
 * in Phase 2 this becomes a JPA @Entity, which needs mutable state and a non-final class.
 */
public class Issuance {

    private static final Pattern ISIN = Pattern.compile("[A-Z]{2}[A-Z0-9]{9}[0-9]");

    private final String id;
    private final IssuanceType type;
    private final String issuer;
    private final String isin;
    private final BigDecimal amount;      // money is always BigDecimal, never double
    private final Currency currency;
    private final LocalDate maturityDate;
    private final String createdBy;
    private final Instant createdAt;

    private Status status;
    private String submittedBy;
    private long version;                 // becomes @Version in Phase 2
    private final List<StatusChange> history;

    public Issuance(IssuanceType type, String issuer, String isin, BigDecimal amount,
                    Currency currency, LocalDate maturityDate, String createdBy) {
        this(UUID.randomUUID().toString(), type, issuer, isin, amount, currency, maturityDate,
                createdBy, Instant.now(), Status.DRAFT, null, 0, new ArrayList<>());
        validate();
    }

    private Issuance(String id, IssuanceType type, String issuer, String isin, BigDecimal amount,
                     Currency currency, LocalDate maturityDate, String createdBy, Instant createdAt,
                     Status status, String submittedBy, long version, List<StatusChange> history) {
        this.id = id;
        this.type = type;
        this.issuer = issuer;
        this.isin = isin;
        this.amount = amount;
        this.currency = currency;
        this.maturityDate = maturityDate;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = status;
        this.submittedBy = submittedBy;
        this.version = version;
        this.history = history;
    }

    private void validate() {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        if (issuer == null || issuer.isBlank()) throw new OversightException.Validation("issuer is required");
        if (isin == null || !ISIN.matcher(isin).matches()) throw new OversightException.Validation("invalid ISIN: " + isin);
        if (amount == null || amount.signum() <= 0) throw new OversightException.Validation("amount must be positive");
        if (maturityDate == null || !maturityDate.isAfter(LocalDate.now()))
            throw new OversightException.Validation("maturity date must be in the future");
    }

    /**
     * The only way status changes. Rules about WHO may do it live in the service's guards;
     * this method only protects the state machine itself and records the audit trail.
     */
    public void moveTo(Status next, String actorId, String comment) {
        if (!status.canMoveTo(next)) {
            throw new OversightException.InvalidTransition(status, next);
        }
        if (next == Status.SUBMITTED) {
            submittedBy = actorId;
        }
        history.add(new StatusChange(status, next, actorId, Instant.now(), comment));
        status = next;
    }

    /** True if this user already approved at any level (used to block one person approving twice). */
    public boolean wasApprovedBy(String actorId) {
        return history.stream().anyMatch(c -> c.actorId().equals(actorId)
                && (c.to() == Status.L1_APPROVED || c.to() == Status.L2_APPROVED));
    }

    /**
     * Deep copy with a given version. The repository stores and returns copies, the same way
     * a database hands you a fresh object per load, so two callers never share one instance.
     */
    public Issuance copyWithVersion(long newVersion) {
        return new Issuance(id, type, issuer, isin, amount, currency, maturityDate, createdBy,
                createdAt, status, submittedBy, newVersion, new ArrayList<>(history));
    }

    public String getId() { return id; }
    public IssuanceType getType() { return type; }
    public String getIssuer() { return issuer; }
    public String getIsin() { return isin; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public String getSubmittedBy() { return submittedBy; }
    public long getVersion() { return version; }
    public List<StatusChange> getHistory() { return List.copyOf(history); }

    @Override
    public String toString() {
        return "Issuance[" + type + " " + isin + " " + amount.toPlainString() + " " + currency
                + " status=" + status + " v" + version + "]";
    }
}
