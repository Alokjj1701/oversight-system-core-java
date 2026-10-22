package com.oversight.issuance.service;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Role;
import com.oversight.issuance.domain.Status;
import com.oversight.issuance.domain.OversightException;
import com.oversight.issuance.domain.User;

import java.util.List;

/** The standard rule set. */
public final class Guards {

    private Guards() {
    }

    public static List<TransitionGuard> standard() {
        return List.of(new RoleGuard(), new MakerCheckerGuard(), new NoDoubleApprovalGuard());
    }

    /** Each move needs a specific role. Rejecting needs the role of the level being decided. */
    public static final class RoleGuard implements TransitionGuard {
        @Override
        public void check(Issuance issuance, Status next, User actor) {
            Role required = switch (next) {
                case SUBMITTED -> Role.SUBMITTER;
                case L1_APPROVED -> Role.APPROVER_L1;
                case L2_APPROVED -> Role.APPROVER_L2;
                case PUBLISHED -> Role.PUBLISHER;
                case REJECTED -> issuance.getStatus() == Status.SUBMITTED ? Role.APPROVER_L1 : Role.APPROVER_L2;
                case DRAFT -> throw new OversightException.Forbidden("nobody can move an issuance back to DRAFT");
            };
            if (!actor.hasRole(required)) {
                throw new OversightException.Forbidden(actor.id() + " needs role " + required + " to move to " + next);
            }
        }
    }

    /** Maker-checker: whoever submitted can't approve or reject their own issuance. */
    public static final class MakerCheckerGuard implements TransitionGuard {
        @Override
        public void check(Issuance issuance, Status next, User actor) {
            boolean decision = next == Status.L1_APPROVED || next == Status.L2_APPROVED || next == Status.REJECTED;
            if (decision && actor.id().equals(issuance.getSubmittedBy())) {
                throw new OversightException.Forbidden("maker-checker: " + actor.id() + " submitted this issuance");
            }
        }
    }

    /** The same person can't give both the L1 and the L2 approval. */
    public static final class NoDoubleApprovalGuard implements TransitionGuard {
        @Override
        public void check(Issuance issuance, Status next, User actor) {
            if (next == Status.L2_APPROVED && issuance.wasApprovedBy(actor.id())) {
                throw new OversightException.Forbidden(actor.id() + " already approved this issuance at L1");
            }
        }
    }
}
