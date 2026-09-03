package com.trustgate.domain;

import com.trustgate.crypto.AesGcmStringConverter;
import com.trustgate.domain.enums.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "totp_secret_enc", columnDefinition = "BYTEA")
    private String totpSecretEnc;

    // FIX FOR LINE 39: Added columnDefinition = "BYTEA" to ensure binary storage
    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "kyc_reference_enc", columnDefinition = "BYTEA")
    private String kycReferenceEnc;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // --- FIX FOR LINES 30-33: Added Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getTotpSecretEnc() { return totpSecretEnc; }
    public void setTotpSecretEnc(String totpSecretEnc) { this.totpSecretEnc = totpSecretEnc; }
    
    public String getKycReferenceEnc() { return kycReferenceEnc; }
    public void setKycReferenceEnc(String kycReferenceEnc) { this.kycReferenceEnc = kycReferenceEnc; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}