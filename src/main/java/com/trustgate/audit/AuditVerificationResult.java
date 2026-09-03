package com.trustgate.audit;

import java.util.UUID;

// FIX: Renamed component from 'intact' to 'isIntact' to avoid clashing with the static factory method
public record AuditVerificationResult(boolean isIntact, UUID brokenAtEntryId) {
    
    public static AuditVerificationResult intact() {
        return new AuditVerificationResult(true, null);
    }

    public static AuditVerificationResult brokenAt(UUID entryId) {
        return new AuditVerificationResult(false, entryId);
    }
}