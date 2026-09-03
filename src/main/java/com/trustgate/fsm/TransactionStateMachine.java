package com.trustgate.fsm;

import com.trustgate.domain.enums.TransactionStatus;
import com.trustgate.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TransactionStateMachine {

    // Explicit transition matrix based on 03_API_Specification and 04_Sequence_Diagrams
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = Map.of(
            TransactionStatus.PAYMENT_CONFIRMED, Set.of(TransactionStatus.PACKAGING, TransactionStatus.SHIPPING, TransactionStatus.DISPUTED),
            TransactionStatus.PACKAGING, Set.of(TransactionStatus.SHIPPING, TransactionStatus.DISPUTED),
            TransactionStatus.SHIPPING, Set.of(TransactionStatus.DELIVERED, TransactionStatus.DISPUTED),
            TransactionStatus.DELIVERED, Set.of(TransactionStatus.ACCEPTED, TransactionStatus.DISPUTED)
            // ACCEPTED and DISPUTED are terminal states for the Transaction entity
    );

    /**
     * Validates if a transaction can move from its current state to a requested state.
     * 
     * @param current   The current status of the transaction.
     * @param requested The desired new status.
     * @throws IllegalStateTransitionException if the transition is not permitted.
     */
    public void validateTransition(TransactionStatus current, TransactionStatus requested) {
        Set<TransactionStatus> allowedNextStates = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        
        if (!allowedNextStates.contains(requested)) {
            throw new IllegalStateTransitionException(
                    String.format("Invalid state transition: Cannot move from %s to %s", current, requested)
            );
        }
    }
}