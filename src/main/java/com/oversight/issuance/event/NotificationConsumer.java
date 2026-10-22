package com.oversight.issuance.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Tells the next group of people they have work to do. Phase 5: notification-service (email/Teams). */
public class NotificationConsumer extends IdempotentConsumer {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    @Override
    protected void process(IssuanceEvent event) {
        String message = switch (event.to()) {
            case SUBMITTED -> "L1 approvers: issuance " + event.issuanceId() + " awaits your approval";
            case L1_APPROVED -> "L2 approvers: issuance " + event.issuanceId() + " awaits final approval";
            case L2_APPROVED -> "Publishers: issuance " + event.issuanceId() + " is ready to publish";
            case REJECTED -> "Submitter: issuance " + event.issuanceId() + " was rejected";
            case PUBLISHED -> "All: issuance " + event.issuanceId() + " is published";
            case DRAFT -> null;
        };
        if (message != null) {
            sent.add(message);
            System.out.println("  [notify] " + message);
        }
    }

    public List<String> sentMessages() {
        return List.copyOf(sent);
    }
}
