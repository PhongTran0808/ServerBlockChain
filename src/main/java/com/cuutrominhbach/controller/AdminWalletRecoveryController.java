package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.RecoverWalletRequest;
import com.cuutrominhbach.dto.response.RecoverWalletResponse;
import com.cuutrominhbach.service.WalletRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminWalletRecoveryController {

    private final WalletRecoveryService walletRecoveryService;

    public AdminWalletRecoveryController(WalletRecoveryService walletRecoveryService) {
        this.walletRecoveryService = walletRecoveryService;
    }

    @PostMapping("/recover-wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecoverWalletResponse> recoverWallets(
            @Valid @RequestBody RecoverWalletRequest request) {
        return ResponseEntity.ok(walletRecoveryService.recover(request));
    }
}
