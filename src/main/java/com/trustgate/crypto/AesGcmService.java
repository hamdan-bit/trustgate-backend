package com.trustgate.crypto;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

@Service
public class AesGcmService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12; // 96 bits
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey aesKey;

    public AesGcmService(SecretKey aesKey) {
        this.aesKey = aesKey;
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Concatenate: IV (12) || Ciphertext || Auth Tag (16, appended by GCM)
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            return encryptedData;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] ivCiphertextTag) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(ivCiphertextTag, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] ciphertextWithTag = new byte[ivCiphertextTag.length - IV_LENGTH_BYTES];
            System.arraycopy(ivCiphertextTag, IV_LENGTH_BYTES, ciphertextWithTag, 0, ciphertextWithTag.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);
            
            // Throws AEADBadTagException if tampered
            return cipher.doFinal(ciphertextWithTag);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed (possible tampering)", e);
        }
    }
}