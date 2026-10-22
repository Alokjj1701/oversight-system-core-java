package com.oversight.issuance.service;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Role;
import com.oversight.issuance.domain.Status;
import com.oversight.issuance.domain.OversightException;
import com.oversight.issuance.domain.User;
import com.oversight.issuance.event.IssuanceEvent;
import com.oversight.issuance.event.Outbox;
import com.oversight.issuance.repository.IssuanceRepository;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * All workflow use cases. Callers pass the version they last saw, so an approval based on a
 * stale screen is rejected instead of silently overwriting someone else's change.
 */
public class IssuanceService {

    private final IssuanceRepository repository;
    private final Outbox outbox;
    private final List<TransitionGuard> guards;

    public IssuanceService(IssuanceRepository repository, Outbox outbox, List<TransitionGuard> guards) {
        this.repository = repository;
        this.outbox = outbox;
        this.guards = List.copyOf(guards);
    }

    public Issuance create(CreateIssuanceRequest request, User actor) {
        if (!actor.hasRole(Role.SUBMITTER)) {
            throw new OversightException.Forbidden(actor.id() + " needs role SUBMITTER to create an issuance");
        }
        Issuance issuance = new Issuance(request.type(), request.issuer(), request.isin(), request.amount(),
                request.currency(), request.maturityDate(), actor.id());
        Issuance saved = repository.save(issuance);
        outbox.add(IssuanceEvent.of(saved, null, actor.id()));
        return saved;
    }

    public Issuance submit(String id, long expectedVersion, User actor) {
        return transition(id, expectedVersion, actor, null, from -> Status.SUBMITTED);
    }

    /** Works out the level from the current status: SUBMITTED gets L1, L1_APPROVED gets L2. */
    public Issuance approve(String id, long expectedVersion, User actor, String comment) {
        return transition(id, expectedVersion, actor, comment, from -> switch (from) {
            case SUBMITTED -> Status.L1_APPROVED;
            case L1_APPROVED -> Status.L2_APPROVED;
            default -> throw new OversightException.InvalidTransition(from, "approve");
        });
    }

    public Issuance reject(String id, long expectedVersion, User actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new OversightException.Validation("a rejection needs a reason");
        }
        return transition(id, expectedVersion, actor, reason, from -> Status.REJECTED);
    }

    public Issuance publish(String id, long expectedVersion, User actor) {
        return transition(id, expectedVersion, actor, null, from -> Status.PUBLISHED);
    }

    public Issuance get(String id) {
        return repository.findById(id).orElseThrow(() -> new OversightException.NotFound(id));
    }

    public List<Issuance> findByStatus(Status status) {
        return repository.findByStatus(status);
    }

    /*
     * Phase 2 puts @Transactional on this so save() and outbox.add() commit together.
     * Here they are two separate steps: a crash between them would lose the event,
     * which is exactly the dual-write problem the outbox table solves.
     */
    private Issuance transition(String id, long expectedVersion, User actor, String comment,
                                UnaryOperator<Status> nextFor) {
        Issuance issuance = get(id);
        if (issuance.getVersion() != expectedVersion) {
            throw new OversightException.StaleVersion(id, expectedVersion, issuance.getVersion());
        }
        Status from = issuance.getStatus();
        Status next = nextFor.apply(from);
        if (!from.canMoveTo(next)) {
            throw new OversightException.InvalidTransition(from, next);
        }
        guards.forEach(g -> g.check(issuance, next, actor));

        issuance.moveTo(next, actor.id(), comment);
        Issuance saved = repository.save(issuance);   // re-checks the version atomically: catches races
        outbox.add(IssuanceEvent.of(saved, from, actor.id()));
        return saved;
    }
}
