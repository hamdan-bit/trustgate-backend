package com.trustgate.repository;

import com.trustgate.domain.SignedAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SignedActionRepository extends JpaRepository<SignedAction, UUID> {
    List<SignedAction> findByTransactionId(UUID transactionId);
}