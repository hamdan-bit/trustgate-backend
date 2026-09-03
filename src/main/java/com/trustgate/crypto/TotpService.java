package com.trustgate.crypto;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.springframework.stereotype.Service;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class TotpService {
    private static final TimeBasedOneTimePasswordGenerator totpGenerator = 
            new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6, "HmacSHA1");

    public String generateSecret() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
            keyGen.init(160); // 160 bits = 20 bytes, standard for TOTP
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    public boolean verify(String base64Secret, String submittedCode) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA1");
            Instant now = Instant.now();

            // RFC 6238: ±1 step tolerance window (30 seconds each side)
            String codePrev = totpGenerator.generateOneTimePasswordString(keySpec, now.minusSeconds(30));
            String codeCurr = totpGenerator.generateOneTimePasswordString(keySpec, now);
            String codeNext = totpGenerator.generateOneTimePasswordString(keySpec, now.plusSeconds(30));

            return submittedCode.equals(codePrev) || submittedCode.equals(codeCurr) || submittedCode.equals(codeNext);
        } catch (Exception e) {
            return false;
        }
    }
}