package com.trustgate.api;

import com.trustgate.audit.AuditLogService;
import com.trustgate.audit.AuditVerificationResult;
import com.trustgate.domain.AuditLogEntry;
import com.trustgate.repository.AuditLogEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuditController {

    private final AuditLogEntryRepository auditRepository;
    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogEntryRepository auditRepository, AuditLogService auditLogService) {
        this.auditRepository = auditRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-log")
    public ResponseEntity<List<Map<String, Object>>> getAuditLog() {
        List<AuditLogEntry> entries = auditRepository.findAllByOrderByCreatedAtAsc();
        List<Map<String, Object>> result = entries.stream().map(entry -> Map.<String, Object>of(
                "id", entry.getId().toString(),
                "action", entry.getAction(),
                "targetId", entry.getTargetId().toString(),
                "entryHash", entry.getEntryHash(),
                "prevEntryHash", entry.getPrevEntryHash() == null ? "" : entry.getPrevEntryHash(),
                "createdAt", entry.getCreatedAt().toString()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit-log/verify")
    public ResponseEntity<Map<String, Object>> verifyAuditLog() {
        AuditVerificationResult result = auditLogService.verifyChain();
        if (result.isIntact()) {
            return ResponseEntity.ok(Map.of("intact", true));
        } else {
            return ResponseEntity.ok(Map.of(
                    "intact", false,
                    "brokenAtEntryId", result.brokenAtEntryId().toString()
            ));
        }
    }
}