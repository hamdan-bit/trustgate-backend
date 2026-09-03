package com.trustgate.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

@Converter
public class AesGcmStringConverter implements AttributeConverter<String, byte[]> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private static final SecretKey AES_KEY = KeyProvider.getAesKey();

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] plaintext = attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, AES_KEY, parameterSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Concatenate IV (12 bytes) || Ciphertext || Auth Tag (16 bytes appended by GCM)
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            return encryptedData;
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting string", e);
        }
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(dbData, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] ciphertextWithTag = new byte[dbData.length - IV_LENGTH_BYTES];
            System.arraycopy(dbData, IV_LENGTH_BYTES, ciphertextWithTag, 0, ciphertextWithTag.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, AES_KEY, parameterSpec);
            
            // Throws AEADBadTagException if data was tampered with
            byte[] plaintext = cipher.doFinal(ciphertextWithTag);

            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting string (possible tampering or wrong key)", e);
        }
    }
}