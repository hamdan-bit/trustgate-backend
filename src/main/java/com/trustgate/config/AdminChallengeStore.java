package com.trustgate.config;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminChallengeStore {

    public record ChallengeEntry(UUID userId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, ChallengeEntry> challenges = new ConcurrentHashMap<>();

    public String createChallenge(UUID userId) {
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, new ChallengeEntry(userId, Instant.now().plusSeconds(300)));
        return challengeId;
    }

    public Optional<UUID> validateChallenge(String challengeId) {
        ChallengeEntry entry = challenges.remove(challengeId);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }
}