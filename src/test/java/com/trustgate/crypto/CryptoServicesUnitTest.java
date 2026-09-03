package com.trustgate.crypto;

import com.trustgate.config.CryptoKeyConfig;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        CryptoKeyConfig.class,
        PasswordService.class,
        AesGcmService.class,
        SignatureService.class,
        TotpService.class,
        JwtService.class
})
public class CryptoServicesUnitTest {

    @Autowired private PasswordService passwordService;
    @Autowired private AesGcmService aesGcmService;
    @Autowired private SignatureService signatureService;
    @Autowired private TotpService totpService;
    @Autowired private JwtService jwtService;

    @Test
    void testPasswordService_RoundTrip() {
        String rawPassword = "SuperSecretPassword123!";
        String hash = passwordService.hash(rawPassword);
        
        assertThat(hash).startsWith("$2a$12$"); // BCrypt strength 12 prefix
        assertTrue(passwordService.matches(rawPassword, hash));
        assertFalse(passwordService.matches("WrongPassword", hash));
    }

    @Test
    void testAesGcmService_RoundTripAndTamperDetection() {
        byte[] plaintext = "CONFIDENTIAL_KYC_DATA".getBytes(StandardCharsets.UTF_8);
        
        // 1. Round-trip encryption/decryption
        byte[] encrypted = aesGcmService.encrypt(plaintext);
        byte[] decrypted = aesGcmService.decrypt(encrypted);
        
        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted.length).isGreaterThan(plaintext.length); // IV + Ciphertext + Tag

        // 2. Tamper Detection: Flip a bit in the ciphertext
        byte[] tampered = encrypted.clone();
        int tamperIndex = Math.min(15, tampered.length - 1);
        tampered[tamperIndex] = (byte) (tampered[tamperIndex] ^ 0xFF);

        // 3. Assert it throws RuntimeException (wrapping AEADBadTagException)
        assertThatThrownBy(() -> aesGcmService.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void testSignatureService_RoundTripAndTamperDetection() {
        // 1. Generate keys
        KeyPair keyPair = signatureService.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // 2. Sign a payload
        byte[] payload = "transaction_123_delivered".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signatureService.sign(payload, privateKey);

        // 3. Verify valid signature
        assertTrue(signatureService.verify(payload, signature, publicKey));

        // 4. Tamper Detection: Alter the payload
        byte[] tamperedPayload = "transaction_123_denied".getBytes(StandardCharsets.UTF_8);
        assertFalse(signatureService.verify(tamperedPayload, signature, publicKey));

        // 5. Tamper Detection: Alter the signature itself
        byte[] tamperedSig = signature.clone();
        tamperedSig[0] = (byte) (tamperedSig[0] ^ 0xFF);
        assertFalse(signatureService.verify(payload, tamperedSig, publicKey));
    }

    @Test
    void testTotpService_GenerationAndVerification() {
        // 1. Generate a secret
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();

        // 2. Verify against a known valid format (we can't easily predict the exact 6-digit code 
        // without calling an internal method, but we can assert the format and that invalid codes fail)
        assertThat(secret).matches("^[A-Za-z0-9+/=]+$"); // Base64 encoded
        
        // 3. Assert that a random wrong code fails
        assertFalse(totpService.verify(secret, "000000"));
        assertFalse(totpService.verify(secret, "999999"));
    }

    @Test
    void testJwtService_IssueAndValidate() {
        // 1. Create a mock user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.BUYER);

        // 2. Issue token
        String token = jwtService.issue(user);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature

        // 3. Validate token
        var claimsOpt = jwtService.validate(token);
        assertTrue(claimsOpt.isPresent());
        
        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("role", String.class)).isEqualTo("BUYER");

        // 4. Tamper/Invalid token detection
        String tamperedToken = token + "x";
        assertTrue(jwtService.validate(tamperedToken).isEmpty());
        
        String wrongToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertTrue(jwtService.validate(wrongToken).isEmpty());
    }
}