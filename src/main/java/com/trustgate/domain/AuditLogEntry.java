package com.trustgate.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    // NEW FIELD: The raw data that was hashed to create the entry_hash
    @Column(name = "entry_data", nullable = false, columnDefinition = "TEXT")
    private String entryData;

    @Column(name = "entry_hash", nullable = false)
    private String entryHash;

    @Column(name = "prev_entry_hash")
    private String prevEntryHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    
    public String getEntryData() { return entryData; }
    public void setEntryData(String entryData) { this.entryData = entryData; }
    
    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }
    
    public String getPrevEntryHash() { return prevEntryHash; }
    public void setPrevEntryHash(String prevEntryHash) { this.prevEntryHash = prevEntryHash; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}