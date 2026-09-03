package com.trustgate.crypto;

import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class CryptoKeyHolder {
    private static SecretKey aesKey;

    public CryptoKeyHolder(SecretKey aesKey) {
        CryptoKeyHolder.aesKey = aesKey;
    }

    public static SecretKey getAesKey() {
        if (aesKey == null) {
            throw new IllegalStateException("AES Key not initialized. Ensure Spring context is loaded.");
        }
        return aesKey;
    }
}
