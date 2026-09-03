package com.trustgate.repository;

import com.trustgate.domain.Transaction;
import com.trustgate.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByBuyerIdOrVendorId(UUID buyerId, UUID vendorId);
    List<Transaction> findByStatus(TransactionStatus status);
}