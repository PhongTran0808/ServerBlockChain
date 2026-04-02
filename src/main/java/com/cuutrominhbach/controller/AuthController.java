package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.LoginRequest;
import com.cuutrominhbach.dto.request.RegisterRequest;
import com.cuutrominhbach.dto.response.LoginResponse;
import com.cuutrominhbach.dto.response.RegisterResponse;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("LOGIN ERROR: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Auto-generate wallet address for any role if not provided
            RegisterRequest finalRequest = request;
            if (request.walletAddress() == null || request.walletAddress().trim().isEmpty()) {
                String autoWallet = generateWalletAddress(request.username() + "_" + request.role());
                finalRequest = new RegisterRequest(request.username(), request.password(), request.fullName(), 
                                                   request.role(), request.province(), autoWallet);
                log.info("Auto-generated wallet address for {} role: {}", request.role(), autoWallet);
            }
            RegisterResponse response = authService.register(finalRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("REGISTER ERROR: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống"));
        }
    }

    private String generateWalletAddress(String username) {
        // Generate deterministic wallet address based on username using SHA-256
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(username.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("0x");
            for (int i = 0; i < Math.min(20, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("Failed to generate wallet address from username, using fallback");
            // Fallback: use part of the username with an Ethereum-like prefix
            return "0x" + String.format("%039s", Integer.toHexString(username.hashCode())).replace(' ', '0');
        }
    }
}
