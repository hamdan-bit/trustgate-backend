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

    // Getters and Setters omitted for brevity.
}