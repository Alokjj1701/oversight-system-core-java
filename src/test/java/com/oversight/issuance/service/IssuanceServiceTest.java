package com.oversight.issuance.service;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.IssuanceType;
import com.oversight.issuance.domain.Role;
import com.oversight.issuance.domain.Status;
import com.oversight.issuance.domain.OversightException;
import com.oversight.issuance.domain.User;
import com.oversight.issuance.event.AuditConsumer;
import com.oversight.issuance.event.NotificationConsumer;
import com.oversight.issuance.event.Outbox;
import com.oversight.issuance.event.OutboxRelay;
import com.oversight.issuance.repository.InMemoryIssuanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuanceServiceTest {

    private final User maker = new User("maker", "Maker", Set.of(Role.SUBMITTER, Role.APPROVER_L1));
    private final User l1 = new User("l1", "L1", Set.of(Role.APPROVER_L1, Role.APPROVER_L2));
    private final User l1b = new User("l1b", "L1 B", Set.of(Role.APPROVER_L1));
    private final User l2 = new User("l2", "L2", Set.of(Role.APPROVER_L2));
    private final User ops = new User("ops", "Ops", Set.of(Role.PUBLISHER));

    private Outbox outbox;
    private IssuanceService service;

    @BeforeEach
    void setUp() {
        outbox = new Outbox();
        service = new IssuanceService(new InMemoryIssuanceRepository(), outbox, Guards.standard());
    }

    private Issuance submitted() {
        Issuance i = service.create(new CreateIssuanceRequest(IssuanceType.TAP, "Acme Bank PLC", "XS2345678901",
                new BigDecimal("250000000"), Currency.getInstance("USD"), LocalDate.now().plusYears(3)), maker);
        return service.submit(i.getId(), i.getVersion(), maker);
    }

    @Test
    void fullWorkflowReachesPublished() {
        Issuance i = submitted();
        i = service.approve(i.getId(), i.getVersion(), l1, "ok");
        i = service.approve(i.getId(), i.getVersion(), l2, "ok");
        i = service.publish(i.getId(), i.getVersion(), ops);

        assertEquals(Status.PUBLISHED, i.getStatus());
        assertEquals(5, i.getVersion());           // create + 4 transitions
        assertEquals(4, i.getHistory().size());
        assertEquals(5, outbox.size());            // one event per saved change
    }

    @Test
    void submitterCannotApproveOwnIssuance() {
        Issuance i = submitted();
        assertThrows(OversightException.Forbidden.class, () -> service.approve(i.getId(), i.getVersion(), maker, "mine"));
    }

    @Test
    void sameUserCannotApproveAtBothLevels() {
        Issuance s = submitted();
        Issuance i = service.approve(s.getId(), s.getVersion(), l1, "ok");
        assertThrows(OversightException.Forbidden.class, () -> service.approve(i.getId(), i.getVersion(), l1, "again"));
    }

    @Test
    void missingRoleIsForbidden() {
        Issuance i = submitted();
        assertThrows(OversightException.Forbidden.class, () -> service.approve(i.getId(), i.getVersion(), ops, "ok"));
    }

    @Test
    void cannotPublishBeforeApprovals() {
        Issuance i = submitted();
        assertThrows(OversightException.InvalidTransition.class, () -> service.publish(i.getId(), i.getVersion(), ops));
    }

    @Test
    void staleVersionIsRejected() {
        Issuance i = submitted();
        assertThrows(OversightException.StaleVersion.class, () -> service.approve(i.getId(), i.getVersion() - 1, l1, "ok"));
    }

    @Test
    void rejectionNeedsAReason() {
        Issuance i = submitted();
        assertThrows(OversightException.Validation.class, () -> service.reject(i.getId(), i.getVersion(), l1, " "));
    }

    @Test
    void invalidIsinIsRejected() {
        assertThrows(OversightException.Validation.class, () -> service.create(new CreateIssuanceRequest(IssuanceType.TAP,
                "Acme Bank", "NOT-AN-ISIN", BigDecimal.TEN, Currency.getInstance("USD"), LocalDate.now().plusYears(1)), maker));
    }

    /** Two approvers click Approve on the same version at the same moment: exactly one may win. */
    @Test
    void concurrentApprovalsOnlyOneWins() throws Exception {
        Issuance i = submitted();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Issuance>> clicks = List.of(
                    () -> { start.await(); return service.approve(i.getId(), i.getVersion(), l1, "first"); },
                    () -> { start.await(); return service.approve(i.getId(), i.getVersion(), l1b, "second"); });
            List<Future<Issuance>> results = new ArrayList<>();
            for (Callable<Issuance> click : clicks) results.add(pool.submit(click));
            start.countDown();

            int wins = 0, conflicts = 0;
            for (Future<Issuance> f : results) {
                try {
                    f.get();
                    wins++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertTrue(e.getCause() instanceof OversightException.StaleVersion);
                    conflicts++;
                }
            }
            assertEquals(1, wins);
            assertEquals(1, conflicts);
            assertEquals(Status.L1_APPROVED, service.get(i.getId()).getStatus());
        } finally {
            pool.shutdownNow();
        }
    }

    /** The relay is at-least-once; consumers must ignore a redelivered event. */
    @Test
    void consumersIgnoreDuplicateEvents() {
        NotificationConsumer notifications = new NotificationConsumer();
        AuditConsumer audit = new AuditConsumer();
        submitted();
        OutboxRelay relay = new OutboxRelay(outbox, List.of(notifications, audit));
        relay.relayOnce(10);

        // Simulate a crash after delivery but before markSent: the same events arrive again.
        audit.handle(audit.entries().get(0));
        audit.handle(audit.entries().get(1));

        assertEquals(2, audit.entries().size());              // created + submitted, no duplicates
        assertEquals(1, notifications.sentMessages().size()); // only SUBMITTED notifies anyone
    }
}
