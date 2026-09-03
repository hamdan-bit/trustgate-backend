package com.trustgate.audit;

import com.trustgate.domain.AuditLogEntry;
import com.trustgate.domain.User;
import com.trustgate.repository.AuditLogEntryRepository;
import com.trustgate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogEntryRepository auditRepository;
    private final UserRepository userRepository;
    private final SecretKey hmacKey;

    // FIX: Rename the parameter to 'hmacJwtKey' to match the @Bean name in CryptoKeyConfig.
    // Spring uses parameter name matching to resolve ambiguity when multiple beans of the same type exist.
    public AuditLogService(AuditLogEntryRepository auditRepository, 
                           UserRepository userRepository, 
                           SecretKey hmacJwtKey) { 
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.hmacKey = hmacJwtKey; // Assign the correctly resolved bean to our internal field
    }

    @Transactional
    public AuditLogEntry append(UUID actorId, String action, UUID targetId, String entryData) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");

        AuditLogEntry latest = auditRepository.findFirstByOrderByCreatedAtDesc().orElse(null);
        String prevHash = (latest != null) ? latest.getEntryHash() : null;

        String dataToHash = (prevHash == null ? "" : prevHash) + entryData;
        String entryHash = computeHmac(dataToHash);

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found"));

        AuditLogEntry entry = new AuditLogEntry();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setTargetId(targetId);
        entry.setEntryData(entryData);
        entry.setEntryHash(entryHash);
        entry.setPrevEntryHash(prevHash);

        return auditRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public AuditVerificationResult verifyChain() {
        List<AuditLogEntry> entries = auditRepository.findAllByOrderByCreatedAtAsc();
        String expectedPrevHash = null;

        for (AuditLogEntry entry : entries) {
            if (expectedPrevHash == null) {
                if (entry.getPrevEntryHash() != null) return AuditVerificationResult.brokenAt(entry.getId());
            } else {
                if (!expectedPrevHash.equals(entry.getPrevEntryHash())) {
                    return AuditVerificationResult.brokenAt(entry.getId());
                }
            }

            String dataToHash = (entry.getPrevEntryHash() == null ? "" : entry.getPrevEntryHash()) + entry.getEntryData();
            String expectedHash = computeHmac(dataToHash);

            if (!expectedHash.equals(entry.getEntryHash())) {
                return AuditVerificationResult.brokenAt(entry.getId());
            }

            expectedPrevHash = entry.getEntryHash();
        }

        return AuditVerificationResult.intact();
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }
}