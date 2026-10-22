package com.oversight.issuance.domain;

/**
 * Base for all business errors. Unchecked on purpose: in Phase 2, @Transactional only
 * rolls back on unchecked exceptions by default, and @RestControllerAdvice maps each
 * subclass to an HTTP status.
 */
public class OversightException extends RuntimeException {

    public OversightException(String message) {
        super(message);
    }

    /** -> 404 */
    public static class NotFound extends OversightException {
        public NotFound(String id) { super("Issuance not found: " + id); }
    }

    /** -> 409. Someone else changed the issuance after you loaded it. */
    public static class StaleVersion extends OversightException {
        public StaleVersion(String id, long expected, long actual) {
            super("Issuance " + id + " was modified (expected version " + expected + ", found " + actual + ")");
        }
    }

    /** -> 409. The state machine does not allow this move. */
    public static class InvalidTransition extends OversightException {
        public InvalidTransition(Status from, Status to) { super(from + " -> " + to + " is not allowed"); }
        public InvalidTransition(Status from, String action) { super("cannot " + action + " an issuance in status " + from); }
    }

    /** -> 403. Missing role, maker-checker violation, or approving twice. */
    public static class Forbidden extends OversightException {
        public Forbidden(String message) { super(message); }
    }

    /** -> 400 */
    public static class Validation extends OversightException {
        public Validation(String message) { super(message); }
    }
}
