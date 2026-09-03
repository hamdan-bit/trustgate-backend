package com.trustgate.api;

import com.trustgate.crypto.PasswordService;
import com.trustgate.crypto.JwtService;
import com.trustgate.domain.User;
import com.trustgate.domain.enums.UserRole;
import com.trustgate.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordService passwordService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String roleStr = (String) request.get("role");
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordService.hash(password));
        user.setRole(UserRole.valueOf(roleStr.toUpperCase()));

        userRepository.save(user);
        return ResponseEntity.status(201).body(Map.of("userId", user.getId().toString()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(request.get("email"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordService.matches(request.get("password"), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.issue(user);
        return ResponseEntity.ok(Map.of("token", token));
    }
}