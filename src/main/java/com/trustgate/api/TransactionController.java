package com.trustgate.api;

import com.trustgate.crypto.SignatureService;
import com.trustgate.domain.SignedAction;
import com.trustgate.domain.Transaction;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.SignedActionType;
import com.trustgate.domain.enums.TransactionStatus;
import com.trustgate.fsm.TransactionStateMachine;
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
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository txRepository;
    private final UserRepository userRepository;
    private final SignedActionRepository signedActionRepository;
    private final TransactionStateMachine fsm;
    private final SignatureService signatureService;

    // FIX 1: Removed AesGcmService from the constructor as it is no longer used here.
    // The @Convert annotation on the Transaction entity handles encryption transparently.
    public TransactionController(TransactionRepository txRepository, UserRepository userRepository,
                                 SignedActionRepository signedActionRepository, TransactionStateMachine fsm,
                                 SignatureService signatureService) {
        this.txRepository = txRepository;
        this.userRepository = userRepository;
        this.signedActionRepository = signedActionRepository;
        this.fsm = fsm;
        this.signatureService = signatureService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody Map<String, Object> request, Authentication auth) {
        UUID buyerId = UUID.fromString(Objects.requireNonNull(auth.getName()));
        UUID vendorId = UUID.fromString(Objects.requireNonNull((String) request.get("vendorId")));
        String description = (String) request.get("description");

        Transaction tx = new Transaction();
        
        // FIX 2: Wrapped the UUID variables directly in Objects.requireNonNull() 
        // to satisfy Spring Data JPA's strict @NonNull parameter expectations.
        tx.setBuyer(userRepository.getReferenceById(Objects.requireNonNull(buyerId)));
        tx.setVendor(userRepository.getReferenceById(Objects.requireNonNull(vendorId)));
        
        tx.setDescriptionEnc(description); 
        tx.setStatus(TransactionStatus.PAYMENT_CONFIRMED);

        txRepository.save(tx);
        return ResponseEntity.status(201).body(Map.of("transactionId", tx.getId().toString(), "status", tx.getStatus().name()));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<Map<String, String>> ship(@PathVariable UUID id, Authentication auth) {
        Transaction tx = txRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        fsm.validateTransition(tx.getStatus(), TransactionStatus.SHIPPING);
        
        tx.setStatus(TransactionStatus.SHIPPING);
        txRepository.save(tx);
        return ResponseEntity.ok(Map.of("status", tx.getStatus().name()));
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<Map<String, String>> confirmDelivery(@PathVariable UUID id, @RequestBody Map<String, String> request, Authentication auth) {
        Transaction tx = txRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        String buyerIdStr = Objects.requireNonNull(auth.getName());
        
        // FIX 3: Wrapped the UUID.fromString() call directly in Objects.requireNonNull()
        User buyer = userRepository.findById(Objects.requireNonNull(UUID.fromString(buyerIdStr)))
                .orElseThrow();

        fsm.validateTransition(tx.getStatus(), TransactionStatus.ACCEPTED);

        String signatureBase64 = request.get("signature");
        String payload = tx.getId().toString() + ":" + Instant.now().getEpochSecond(); 
        
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        boolean isValid = signatureService.verify(payload.getBytes(StandardCharsets.UTF_8), sigBytes, signatureService.pemToPublicKey(buyer.getPublicKey()));

        if (!isValid) {
            throw new IllegalArgumentException("Invalid delivery confirmation signature");
        }

        SignedAction action = new SignedAction();
        action.setTransaction(tx);
        action.setActor(buyer);
        action.setActionType(SignedActionType.DELIVERY_CONFIRM);
        action.setPayload(payload);
        action.setSignature(sigBytes);
        signedActionRepository.save(action);

        tx.setStatus(TransactionStatus.ACCEPTED);
        txRepository.save(tx);

        return ResponseEntity.ok(Map.of("status", tx.getStatus().name()));
    }
}