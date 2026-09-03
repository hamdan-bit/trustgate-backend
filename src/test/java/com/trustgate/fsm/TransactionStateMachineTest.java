package com.trustgate.fsm;

import com.trustgate.domain.enums.TransactionStatus;
import com.trustgate.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransactionStateMachineTest {

    private final TransactionStateMachine fsm = new TransactionStateMachine();

    @Test
    void testAllLegalTransitions() {
        // Happy path: Normal lifecycle
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.PAYMENT_CONFIRMED, TransactionStatus.PACKAGING));
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.PAYMENT_CONFIRMED, TransactionStatus.SHIPPING)); // Skip packaging if allowed
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.PACKAGING, TransactionStatus.SHIPPING));
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.SHIPPING, TransactionStatus.DELIVERED));
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.DELIVERED, TransactionStatus.ACCEPTED));
        
        // Dispute path: Can be triggered from active states
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.SHIPPING, TransactionStatus.DISPUTED));
        assertDoesNotThrow(() -> fsm.validateTransition(TransactionStatus.DELIVERED, TransactionStatus.DISPUTED));
    }

    @Test
    void testIllegalTransitions() {
        // 1. Same-state no-op (Must throw)
        assertThrows(IllegalStateTransitionException.class, 
            () -> fsm.validateTransition(TransactionStatus.SHIPPING, TransactionStatus.SHIPPING));
        
        // 2. Skip a step (Must throw)
        assertThrows(IllegalStateTransitionException.class, 
            () -> fsm.validateTransition(TransactionStatus.PAYMENT_CONFIRMED, TransactionStatus.DELIVERED));
            
        // 3. Backwards transition (Must throw)
        assertThrows(IllegalStateTransitionException.class, 
            () -> fsm.validateTransition(TransactionStatus.DELIVERED, TransactionStatus.SHIPPING));
            
        // 4. Transition from a terminal state (Must throw)
        assertThrows(IllegalStateTransitionException.class, 
            () -> fsm.validateTransition(TransactionStatus.ACCEPTED, TransactionStatus.SHIPPING));
            
        // 5. Transition from DISPUTED (Terminal for Transaction entity)
        assertThrows(IllegalStateTransitionException.class, 
            () -> fsm.validateTransition(TransactionStatus.DISPUTED, TransactionStatus.DELIVERED));
    }
}