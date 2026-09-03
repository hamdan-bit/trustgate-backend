package com.trustgate.domain;

import com.trustgate.domain.enums.SignedActionType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signed_actions")
public class SignedAction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private SignedActionType actionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private byte[] signature;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @PrePersist
    protected void onCreate() {
        signedAt = Instant.now();
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public SignedActionType getActionType() { return actionType; }
    public void setActionType(SignedActionType actionType) { this.actionType = actionType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public byte[] getSignature() { return signature; }
    public void setSignature(byte[] signature) { this.signature = signature; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
}