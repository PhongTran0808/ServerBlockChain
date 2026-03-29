package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.TransactionResponse;
import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(@RequestBody Map<String, Object> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long amount = Long.valueOf(body.get("amount").toString());
        String pin = (String) body.get("pin");
        return ResponseEntity.ok(walletService.topUp(userId, amount, pin));
    }

    @PostMapping("/donate")
    public ResponseEntity<?> donate(@RequestBody Map<String, Object> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String province = (String) body.get("province");
        Long amount = Long.valueOf(body.get("amount").toString());
        String pin = (String) body.get("pin");
        return ResponseEntity.ok(walletService.donate(userId, province, amount, pin));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long amount = Long.valueOf(body.get("amount").toString());
        String pin = (String) body.get("pin");
        return ResponseEntity.ok(walletService.withdraw(userId, amount, pin));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<TransactionHistory> txs = walletService.getTransactions(userId);
        return ResponseEntity.ok(txs.stream().map(TransactionResponse::from).collect(Collectors.toList()));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({com.cuutrominhbach.exception.AuthException.class})
    public ResponseEntity<Map<String, String>> handleAuth(com.cuutrominhbach.exception.AuthException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("gas price") || msg.contains("replace existing tx") || msg.contains("nonce") || msg.contains("transaction gas")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Mạng Blockchain đang bận xử lý lô trước đó. Vui lòng thao tác chậm lại khoảng 3-5 giây để mạng đồng bộ."));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Giao dịch lỗi: " + (ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định")));
    }
}
