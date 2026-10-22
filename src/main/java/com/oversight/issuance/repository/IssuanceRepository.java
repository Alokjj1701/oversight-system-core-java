package com.oversight.issuance.repository;

import com.oversight.issuance.domain.Issuance;
import com.oversight.issuance.domain.Status;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction. Phase 1 uses an in-memory map; Phase 2 swaps in Spring Data JPA
 * without touching the service, because the service only knows this interface.
 */
public interface IssuanceRepository {

    /**
     * Inserts or updates. Fails with StaleVersion if the stored version differs from the
     * one passed in (optimistic locking). Returns the saved copy with the new version.
     */
    Issuance save(Issuance issuance);

    Optional<Issuance> findById(String id);

    List<Issuance> findByStatus(Status status);
}
