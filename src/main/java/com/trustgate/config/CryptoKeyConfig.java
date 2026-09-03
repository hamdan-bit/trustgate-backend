package com.trustgate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.security.KeyStore;

@Configuration
public class CryptoKeyConfig {

    @Value("${trustgate.keystore.path:classpath:keystore.p12}")
    private String keystorePath;

    @Value("${trustgate.keystore.password:changeit}")
    private String keystorePassword;

    @Bean
    public SecretKey aesKey() {
        return loadKey("aes-key", "AES");
    }

    @Bean
    public SecretKey hmacJwtKey() {
        return loadKey("hmac-jwt-key", "AES"); // We use the raw bytes of this AES key for HMAC-SHA256
    }

    private SecretKey loadKey(String alias, String algorithm) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String path = keystorePath.replace("classpath:", "");
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalStateException("Keystore not found at: " + keystorePath);
                }
                keyStore.load(is, keystorePassword.toCharArray());
                return (SecretKey) keyStore.getKey(alias, keystorePassword.toCharArray());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cryptographic key: " + alias, e);
        }
    }
}
