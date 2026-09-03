package com.trustgate.repository;

import com.trustgate.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {
    Optional<AuditLogEntry> findFirstByOrderByCreatedAtDesc();
    List<AuditLogEntry> findAllByOrderByCreatedAtAsc();
}