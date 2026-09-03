package com.trustgate.domain;

import com.trustgate.crypto.AesGcmStringConverter;
import com.trustgate.domain.enums.DisputeVerdict;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_cases")
public class DisputeCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "buyer_complaint_enc", nullable = false, columnDefinition = "BYTEA")
    private String buyerComplaintEnc;

    @Enumerated(EnumType.STRING)
    private DisputeVerdict verdict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public String getBuyerComplaintEnc() { return buyerComplaintEnc; }
    public void setBuyerComplaintEnc(String buyerComplaintEnc) { this.buyerComplaintEnc = buyerComplaintEnc; }

    public DisputeVerdict getVerdict() { return verdict; }
    public void setVerdict(DisputeVerdict verdict) { this.verdict = verdict; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}