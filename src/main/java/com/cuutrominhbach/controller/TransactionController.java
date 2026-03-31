package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.TransactionStatementResponse;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final com.cuutrominhbach.service.WalletService walletService;

    public TransactionController(TransactionService transactionService, UserRepository userRepository, com.cuutrominhbach.service.WalletService walletService) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionStatementResponse>> getTransactionHistory() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(userId).orElseThrow();
        
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        List<TransactionStatementResponse> statements = transactionService.getHistory(userId, isAdmin);
        
        return ResponseEntity.ok(statements);
    }

    @org.springframework.web.bind.annotation.PostMapping("/pay-shop-direct")
    public ResponseEntity<?> payShopDirect(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long shopId = Long.valueOf(body.get("shopId").toString());
        Long amount = Long.valueOf(body.get("amount").toString());
        String pin = (String) body.get("pin");
        return ResponseEntity.ok(walletService.payShopDirect(userId, shopId, amount, pin));
    }
}
