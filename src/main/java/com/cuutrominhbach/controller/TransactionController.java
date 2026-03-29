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

    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionStatementResponse>> getTransactionHistory() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(userId).orElseThrow();
        
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        List<TransactionStatementResponse> statements = transactionService.getHistory(userId, isAdmin);
        
        return ResponseEntity.ok(statements);
    }
}
