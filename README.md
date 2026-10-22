# Oversight System - Core Java

A learning project: a debt-issuance approval workflow (submit → L1 approval → L2 approval → publish) built first in plain Java 17, then evolved step by step into Spring Boot microservices with Kafka, Redis, Docker and Kubernetes.

This repository is **Phase 1**: the core domain in plain Java with no frameworks, so every concept is visible in the code instead of hidden behind annotations.

## What Phase 1 covers

- **State machine:** allowed status moves live in a table (`Status`), so steps can't be skipped.
- **Approval rules as pluggable guards:** role checks, maker-checker (the submitter can't approve their own issuance), and no single person approving at both levels. Adding a rule means adding a class.
- **Optimistic locking:** every save checks the version the caller last saw, so two people approving at the same moment can't both succeed. A JUnit test races two threads to prove it.
- **Transactional outbox (in memory):** every change records an event; a relay delivers events to consumers.
- **Idempotent consumers:** delivery is at-least-once, so consumers remember processed event IDs and ignore duplicates.
- **Java 17:** records, switch expressions, `Stream.toList()`, `BigDecimal` for money, defensive copies.

## Layout

```
src/main/java/com/oversight/issuance/
  domain/       Issuance, Status (state machine), Role, User, StatusChange, IssuanceType
  service/      IssuanceService, TransitionGuard + guards (role, maker-checker, no double approval)
  repository/   IssuanceRepository + in-memory implementation with optimistic version check
  event/        IssuanceEvent, Outbox, OutboxRelay, consumers (notification, audit)
  OversightDemo.java   runnable walkthrough
src/test/java/  JUnit 5 tests, including a two-thread race on the same approval
```

## Run

Requires JDK 17+ and Maven.

```bash
mvn test
mvn -q compile exec:java
```

The demo creates an issuance, walks it through every approval, and shows the blocked actions (self-approval, double approval, stale version) along with the audit trail.

## Roadmap

| Phase | What gets built |
|---|---|
| **1. Core Java** (this repo) | Domain model, state machine, guards, optimistic locking, in-memory outbox, idempotent consumers |
| 2. Spring Boot | REST API, Spring Data JPA + MySQL, Flyway, Bean Validation, `@RestControllerAdvice`, `@Transactional`, `@Version` |
| 3. Security | Spring Security 6, JWT resource server, role-based access, maker-checker in `@PreAuthorize` |
| 4. Kafka | Outbox table + relay, topics keyed by issuance ID, idempotent consumers, retries and dead-letter topics |
| 5. Microservices | Issuance, notification, audit and reporting services; shared Spring Boot starter |
| 6. Performance | Redis cache-aside for reference data, indexing, N+1 fixes, load test with p95 before/after |
| 7. Delivery & ops | Docker, docker-compose, Kubernetes with probes, CI pipeline, Prometheus + Grafana |
| 8. UI (optional) | Small React screen for submit/approve |

## Author

Alok Narayan Sharma ([@Alokjj1701](https://github.com/Alokjj1701))

## License

[MIT](LICENSE)
