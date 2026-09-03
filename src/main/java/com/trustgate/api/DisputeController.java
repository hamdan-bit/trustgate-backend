package com.trustgate.api;

import com.trustgate.audit.AuditLogService;
import com.trustgate.crypto.SignatureService;
import com.trustgate.domain.DisputeCase;
import com.trustgate.domain.SignedAction;
import com.trustgate.domain.Transaction;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.DisputeVerdict;
import com.trustgate.domain.enums.SignedActionType;
import com.trustgate.domain.enums.TransactionStatus;
import com.trustgate.fsm.TransactionStateMachine;
import com.trustgate.repository.DisputeCaseRepository;
import com.trustgate.repository.SignedActionRepository;
import com.trustgate.repository.TransactionRepository;
import com.trustgate.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DisputeController {

    private final TransactionRepository txRepository;
    private final UserRepository userRepository;
    private final DisputeCaseRepository disputeRepository;
    private final SignedActionRepository signedActionRepository;
    private final TransactionStateMachine fsm;
    private final SignatureService signatureService;
    private final AuditLogService auditLogService;

    public DisputeController(TransactionRepository txRepository, UserRepository userRepository,
                             DisputeCaseRepository disputeRepository, SignedActionRepository signedActionRepository,
                             TransactionStateMachine fsm, SignatureService signatureService,
                             AuditLogService auditLogService) {
        this.txRepository = txRepository;
        this.userRepository = userRepository;
        this.disputeRepository = disputeRepository;
        this.signedActionRepository = signedActionRepository;
        this.fsm = fsm;
        this.signatureService = signatureService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/transactions/{id}/dispute")
    public ResponseEntity<Map<String, Object>> openDispute(@PathVariable UUID id,
                                                           @RequestBody Map<String, String> request,
                                                           Authentication auth) {
        Transaction tx = txRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // FSM enforces: can only dispute from active states
        fsm.validateTransition(tx.getStatus(), TransactionStatus.DISPUTED);

        if (disputeRepository.findByTransactionId(id).isPresent()) {
            throw new IllegalStateException("Dispute already exists for this transaction");
        }

        DisputeCase dispute = new DisputeCase();
        dispute.setTransaction(tx);
        // The @Convert annotation on buyerComplaintEnc handles AES-GCM encryption transparently
        dispute.setBuyerComplaintEnc(request.get("complaint"));
        disputeRepository.save(dispute);

        tx.setStatus(TransactionStatus.DISPUTED);
        txRepository.save(tx);

        return ResponseEntity.status(201).body(Map.of(
                "disputeId", dispute.getId().toString(),
                "status", "DISPUTED"
        ));
    }

    @PostMapping("/disputes/{id}/verdict")
    public ResponseEntity<Map<String, String>> issueVerdict(@PathVariable UUID id,
                                                            @RequestBody Map<String, String> request,
                                                            Authentication auth) {
        DisputeCase dispute = disputeRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        if (dispute.getVerdict() != null) {
            throw new IllegalStateException("Dispute already resolved");
        }

        String adminIdStr = Objects.requireNonNull(auth.getName());
        User admin = userRepository.findById(Objects.requireNonNull(UUID.fromString(adminIdStr)))
                .orElseThrow();

        DisputeVerdict verdict = DisputeVerdict.valueOf(request.get("verdict"));
        String justification = request.get("justification");
        String signatureBase64 = request.get("signature");
        String timestamp = request.get("timestamp");

        // Non-repudiation: Verify admin's ECDSA signature over the verdict payload (Mitigates Threat T10)
        String payload = dispute.getId().toString() + ":" + verdict.name() + ":" + justification + ":" + timestamp;
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);

        boolean isValid = signatureService.verify(
                payload.getBytes(StandardCharsets.UTF_8),
                sigBytes,
                signatureService.pemToPublicKey(admin.getPublicKey())
        );

        if (!isValid) {
            throw new IllegalArgumentException("Invalid verdict signature");
        }

        // Persist the signed action
        SignedAction action = new SignedAction();
        action.setTransaction(dispute.getTransaction());
        action.setActor(admin);
        action.setActionType(SignedActionType.DISPUTE_VERDICT);
        action.setPayload(payload);
        action.setSignature(sigBytes);
        signedActionRepository.save(action);

        // Set the verdict
        dispute.setVerdict(verdict);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(Instant.now());
        disputeRepository.save(dispute);

        // Append to HMAC-chained audit log (Mitigates Threat T4)
        String entryData = "VERDICT_ISSUED:" + dispute.getId() + ":" + verdict.name() + ":" + justification;
        auditLogService.append(admin.getId(), "VERDICT_ISSUED", dispute.getTransaction().getId(), entryData);

        return ResponseEntity.ok(Map.of("status", "RESOLVED"));
    }
}