package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.LoginRequest;
import com.cuutrominhbach.dto.request.RegisterRequest;
import com.cuutrominhbach.dto.response.LoginResponse;
import com.cuutrominhbach.dto.response.RegisterResponse;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.security.JwtTokenProvider;
import com.cuutrominhbach.service.AuthService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
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
            RegisterResponse response = authService.register(request);
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

    /** Làm mới token — citizen gọi sau khi admin cấp ví để JWT có walletAddress mới */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Thiếu token"));
            }
            String oldToken = header.substring(7);
            Claims claims = jwtTokenProvider.parseToken(oldToken);
            Long userId = Long.valueOf(claims.get("userId").toString());

            // Lấy user mới nhất từ DB (có walletAddress đã được cập nhật)
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthException("Không tìm thấy người dùng"));

            String newToken = jwtTokenProvider.generateToken(user);
            return ResponseEntity.ok(Map.of("token", newToken, "walletAddress", user.getWalletAddress() != null ? user.getWalletAddress() : ""));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token không hợp lệ"));
        }
    }
}
