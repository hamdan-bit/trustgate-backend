package com.trustgate.repository;

import com.trustgate.domain.DisputeCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DisputeCaseRepository extends JpaRepository<DisputeCase, UUID> {
    Optional<DisputeCase> findByTransactionId(UUID transactionId);
}