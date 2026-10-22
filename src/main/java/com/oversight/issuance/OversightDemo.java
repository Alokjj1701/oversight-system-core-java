package com.oversight.issuance;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.IssuanceType;
import com.oversight.issuance.domain.Role;
import com.oversight.issuance.domain.StatusChange;
import com.oversight.issuance.domain.OversightException;
import com.oversight.issuance.domain.User;
import com.oversight.issuance.event.AuditConsumer;
import com.oversight.issuance.event.NotificationConsumer;
import com.oversight.issuance.event.Outbox;
import com.oversight.issuance.event.OutboxRelay;
import com.oversight.issuance.repository.InMemoryIssuanceRepository;
import com.oversight.issuance.service.CreateIssuanceRequest;
import com.oversight.issuance.service.Guards;
import com.oversight.issuance.service.IssuanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;

/** Walks one issuance through the whole workflow, including the things that must be blocked. */
public class OversightDemo {

    public static void main(String[] args) {
        // Wiring by hand. In Phase 2 Spring does this for us (dependency injection).
        Outbox outbox = new Outbox();
        NotificationConsumer notifications = new NotificationConsumer();
        AuditConsumer audit = new AuditConsumer();
        OutboxRelay relay = new OutboxRelay(outbox, List.of(notifications, audit));
        IssuanceService service = new IssuanceService(new InMemoryIssuanceRepository(), outbox, Guards.standard());

        User maker = new User("priya", "Priya (treasury)", Set.of(Role.SUBMITTER, Role.APPROVER_L1));
        User l1 = new User("rahul", "Rahul (L1)", Set.of(Role.APPROVER_L1, Role.APPROVER_L2));
        User l2 = new User("meera", "Meera (L2)", Set.of(Role.APPROVER_L2));
        User ops = new User("arjun", "Arjun (ops)", Set.of(Role.PUBLISHER));

        step("Priya creates a 500m EUR new issuance");
        Issuance i = service.create(new CreateIssuanceRequest(IssuanceType.NEW_ISSUANCE, "Acme Bank PLC",
                "XS2345678901", new BigDecimal("500000000"), Currency.getInstance("EUR"),
                LocalDate.now().plusYears(5)), maker);
        System.out.println("  " + i);

        step("Priya submits it");
        i = service.submit(i.getId(), i.getVersion(), maker);
        System.out.println("  " + i);
        relay.relayOnce(10);

        step("Priya tries to approve her own issuance (she does have APPROVER_L1)");
        Issuance current = i;
        expectBlocked(() -> service.approve(current.getId(), current.getVersion(), maker, "looks fine"));

        step("Rahul approves at L1");
        i = service.approve(i.getId(), i.getVersion(), l1, "terms checked");
        System.out.println("  " + i);
        relay.relayOnce(10);

        step("Rahul tries to give the L2 approval too");
        Issuance afterL1 = i;
        expectBlocked(() -> service.approve(afterL1.getId(), afterL1.getVersion(), l1, "also fine"));

        step("Meera approves with a stale screen (version 2 instead of 3)");
        expectBlocked(() -> service.approve(afterL1.getId(), 2, l2, "ok"));

        step("Meera reloads and approves at L2");
        i = service.approve(i.getId(), service.get(i.getId()).getVersion(), l2, "approved");
        System.out.println("  " + i);

        step("Arjun publishes");
        i = service.publish(i.getId(), i.getVersion(), ops);
        System.out.println("  " + i);
        relay.relayOnce(10);

        step("Audit trail");
        for (StatusChange c : service.get(i.getId()).getHistory()) {
            System.out.printf("  %-11s -> %-11s by %-6s %s%n", c.from(), c.to(), c.actorId(),
                    c.comment() == null ? "" : "(" + c.comment() + ")");
        }
        System.out.println("  audit consumer received " + audit.entries().size() + " events, outbox holds " + outbox.size());
    }

    private static void step(String title) {
        System.out.println("\n== " + title);
    }

    private static void expectBlocked(Runnable action) {
        try {
            action.run();
            System.out.println("  !! was NOT blocked");
        } catch (OversightException e) {
            System.out.println("  blocked (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }
}
