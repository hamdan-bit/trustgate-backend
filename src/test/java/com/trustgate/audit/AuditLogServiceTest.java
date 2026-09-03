package com.trustgate.audit;

import com.trustgate.config.CryptoKeyConfig;
import com.trustgate.domain.AuditLogEntry;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.UserRole;
import com.trustgate.repository.AuditLogEntryRepository;
import com.trustgate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({CryptoKeyConfig.class, AuditLogService.class})
public class AuditLogServiceTest {

    @Autowired private AuditLogService auditLogService;
    @Autowired private AuditLogEntryRepository auditRepository;
    @Autowired private UserRepository userRepository;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        admin.setEmail("admin@test.com");
        admin.setPasswordHash("dummy");
        admin = userRepository.saveAndFlush(admin);
        adminId = admin.getId();
    }

    @Test
    void testAuditChainIntegrityAndTamperDetection() {
        UUID targetTxId = UUID.randomUUID();

        auditLogService.append(adminId, "STATE_TRANSITION", targetTxId, "PAYMENT_CONFIRMED->SHIPPING");
        auditLogService.append(adminId, "STATE_TRANSITION", targetTxId, "SHIPPING->DELIVERED");
        
        // FIX: Removed the unused 'AuditLogEntry thirdEntry =' assignment
        auditLogService.append(adminId, "VERDICT_ISSUED", targetTxId, "AMICABLE_SPLIT");

        AuditVerificationResult initialCheck = auditLogService.verifyChain();
        
        assertThat(initialCheck.isIntact()).isTrue();
        assertThat(initialCheck.brokenAtEntryId()).isNull();

        // Simulate Database Insider Tampering
        AuditLogEntry tamperedEntry = auditRepository.findAllByOrderByCreatedAtAsc().get(1);
        tamperedEntry.setEntryData("SHIPPING->ACCEPTED"); 
        auditRepository.saveAndFlush(tamperedEntry);

        AuditVerificationResult tamperedCheck = auditLogService.verifyChain();
        
        assertThat(tamperedCheck.isIntact())
                .as("Chain must be marked as broken after data tampering")
                .isFalse();
        
        assertThat(tamperedCheck.brokenAtEntryId())
                .as("The break must be reported exactly at the tampered entry")
                .isEqualTo(tamperedEntry.getId());
    }
}