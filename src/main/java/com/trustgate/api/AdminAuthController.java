package com.trustgate.api;

import com.trustgate.config.AdminChallengeStore;
import com.trustgate.crypto.JwtService;
import com.trustgate.crypto.PasswordService;
import com.trustgate.crypto.TotpService;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.UserRole;
import com.trustgate.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects; // <-- ADDED IMPORT
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TotpService totpService;
    private final JwtService jwtService;
    private final AdminChallengeStore challengeStore;

    public AdminAuthController(UserRepository userRepository, PasswordService passwordService,
                               TotpService totpService, JwtService jwtService,
                               AdminChallengeStore challengeStore) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.totpService = totpService;
        this.jwtService = jwtService;
        this.challengeStore = challengeStore;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(request.get("email"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!passwordService.matches(request.get("password"), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String challengeId = challengeStore.createChallenge(user.getId());
        return ResponseEntity.ok(Map.of("challengeId", challengeId));
    }

    @PostMapping("/verify-totp")
    public ResponseEntity<Map<String, String>> verifyTotp(@RequestBody Map<String, String> request) {
        UUID userId = challengeStore.validateChallenge(request.get("challengeId"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired challenge"));

        // FIX: Wrapped userId in Objects.requireNonNull() to satisfy Spring Data JPA's @NonNull parameter expectation
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!totpService.verify(user.getTotpSecretEnc(), request.get("totpCode"))) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        String token = jwtService.issue(user);
        return ResponseEntity.ok(Map.of("token", token));
    }
}