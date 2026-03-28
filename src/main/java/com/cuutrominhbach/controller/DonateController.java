package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.DonateRequest;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DonateController {

    private final WalletService walletService;

    public DonateController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/donate")
    public ResponseEntity<?> donate(@RequestBody DonateRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(walletService.donate(
                userId,
                request.province(),
                request.amount(),
                request.resolvedPin()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}
