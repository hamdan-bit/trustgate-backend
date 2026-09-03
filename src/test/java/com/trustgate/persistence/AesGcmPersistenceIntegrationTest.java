package com.trustgate.persistence;

import com.trustgate.crypto.AesGcmStringConverter;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.UserRole;
import com.trustgate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class AesGcmPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testFieldLevelEncryptionAndTamperEvidence() {
        // 1. Setup and Save
        String originalKyc = "CONFIDENTIAL_KYC_DATA_12345";
        User user = new User();
        user.setRole(UserRole.VENDOR);
        user.setEmail("vendor@test.com");
        user.setPasswordHash("dummy_hash_for_test");
        user.setKycReferenceEnc(originalKyc);

        // FIX FOR NULL SAFETY: Capture the returned entity which now has the generated ID
        User savedUser = userRepository.saveAndFlush(user);
        UUID userId = savedUser.getId();
        
        // Explicitly tell the IDE and test runner that this is not null
        assertNotNull(userId, "User ID must not be null after saveAndFlush");

        // 2. Read back via JPA (Asserts transparent decryption works)
        User fetchedUser = userRepository.findById(userId).orElseThrow();
        assertThat(fetchedUser.getKycReferenceEnc())
                .as("Decrypted value must match original plaintext")
                .isEqualTo(originalKyc);

        // 3. Fetch raw bytes via Native Query
        Object rawResult = entityManager.createNativeQuery(
                "SELECT kyc_reference_enc FROM users WHERE id = :id")
                .setParameter("id", userId) // Use the safe, non-null userId variable
                .getSingleResult();

        // Safely convert to byte[]
        byte[] rawDbBytes;
        if (rawResult instanceof byte[]) {
            rawDbBytes = (byte[]) rawResult;
        } else {
            // Fallback if the driver returns a wrapper or string
            rawDbBytes = rawResult.toString().getBytes(StandardCharsets.UTF_8);
        }

        // Assert it is not human-readable plaintext
        String rawStringRepresentation = new String(rawDbBytes, StandardCharsets.UTF_8);
        assertThat(rawStringRepresentation)
                .as("Raw DB bytes must NOT contain the plaintext")
                .doesNotContain("CONFIDENTIAL");

        assertTrue(rawDbBytes.length > originalKyc.length(), 
                "Ciphertext should be larger than plaintext due to IV and Auth Tag");

        // 4. Tamper-Evidence Test
        byte[] tamperedBytes = rawDbBytes.clone();
        
        // Flip a bit in the ciphertext area (skip the first 12 bytes which is the IV)
        int tamperIndex = Math.min(15, tamperedBytes.length - 1);
        tamperedBytes[tamperIndex] = (byte) (tamperedBytes[tamperIndex] ^ 0xFF); 

        AesGcmStringConverter converter = new AesGcmStringConverter();
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            converter.convertToEntityAttribute(tamperedBytes);
        });

        // Assert the root cause is the specific GCM authentication failure
        assertThat(exception.getCause().getClass().getName())
                .as("Must throw AEADBadTagException when ciphertext is tampered")
                .contains("AEADBadTagException");
    }
}