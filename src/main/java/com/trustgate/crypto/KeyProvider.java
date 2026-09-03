package com.trustgate.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.security.KeyStore;

public class KeyProvider {
    private static SecretKey aesKey;

    static {
        try {
            char[] password = System.getenv("KEYSTORE_PASSWORD") != null 
                ? System.getenv("KEYSTORE_PASSWORD").toCharArray() 
                : "changeit".toCharArray();
            
            try (InputStream is = KeyProvider.class.getClassLoader().getResourceAsStream("keystore.p12")) {
                if (is != null) {
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(is, password);
                    aesKey = (SecretKey) keyStore.getKey("aes-key", password);
                }
            }
            
            // Fallback for Phase 2 testing if keystore is not yet generated
            if (aesKey == null) {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                aesKey = keyGen.generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AES key", e);
        }
    }

    public static SecretKey getAesKey() {
        return aesKey;
    }
}