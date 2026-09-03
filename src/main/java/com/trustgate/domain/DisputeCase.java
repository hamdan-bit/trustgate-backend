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
    @Column(name = "buyer_complaint_enc", nullable = false)
    private String buyerComplaintEnc;

    @Enumerated(EnumType.STRING)
    private DisputeVerdict verdict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    // Getters and Setters omitted for brevity.
}