package com.oversight.issuance.repository;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Status;
import com.oversight.issuance.domain.OversightException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store that behaves like a table with a @Version column.
 *
 * ConcurrentHashMap.compute() runs atomically per key, so the "check version, then write"
 * step can't be interleaved by another thread. That is the in-memory equivalent of
 * UPDATE issuance SET ..., version = v + 1 WHERE id = ? AND version = v
 */
public class InMemoryIssuanceRepository implements IssuanceRepository {

    private final Map<String, Issuance> rows = new ConcurrentHashMap<>();

    @Override
    public Issuance save(Issuance issuance) {
        Issuance saved = rows.compute(issuance.getId(), (id, stored) -> {
            long current = stored == null ? 0 : stored.getVersion();
            if (issuance.getVersion() != current) {
                throw new OversightException.StaleVersion(id, issuance.getVersion(), current);
            }
            return issuance.copyWithVersion(current + 1);
        });
        return saved.copyWithVersion(saved.getVersion()); // never hand out the stored instance
    }

    @Override
    public Optional<Issuance> findById(String id) {
        Issuance stored = rows.get(id);
        return Optional.ofNullable(stored == null ? null : stored.copyWithVersion(stored.getVersion()));
    }

    @Override
    public List<Issuance> findByStatus(Status status) {
        return rows.values().stream()
                .filter(i -> i.getStatus() == status)
                .map(i -> i.copyWithVersion(i.getVersion()))
                .toList();
    }
}
