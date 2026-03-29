package com.cuutrominhbach.controller;

import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions/public")
public class PublicTransactionController {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final UserRepository userRepository;

    public PublicTransactionController(TransactionHistoryRepository transactionHistoryRepository, UserRepository userRepository) {
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPublicTransactions() {
        // Lấy toàn bộ lịch sử giao dịch, sắp xếp mới nhất lên đầu
        List<TransactionHistory> allTxs = transactionHistoryRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        // Cache users để mapping tên và Role
        Map<Long, User> userCache = new HashMap<>();
        for (TransactionHistory tx : allTxs) {
            if (tx.getFromUserId() != null && !userCache.containsKey(tx.getFromUserId())) {
                userRepository.findById(tx.getFromUserId()).ifPresent(u -> userCache.put(u.getId(), u));
            }
            if (tx.getToUserId() != null && !userCache.containsKey(tx.getToUserId())) {
                userRepository.findById(tx.getToUserId()).ifPresent(u -> userCache.put(u.getId(), u));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<Map<String, Object>> response = allTxs.stream().map(tx -> {
            User fromUser = tx.getFromUserId() != null ? userCache.get(tx.getFromUserId()) : null;
            User toUser = tx.getToUserId() != null ? userCache.get(tx.getToUserId()) : null;

            return Map.<String, Object>of(
                    "id", tx.getId(),
                    "type", tx.getType().name(),
                    "amount", tx.getAmount() != null ? tx.getAmount() : 0,
                    "note", tx.getNote() != null ? tx.getNote() : "",
                    "createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().format(formatter) : "",
                    "txHash", tx.getTxHash() != null ? tx.getTxHash() : "",
                    "from", anonymize(fromUser, "Hệ thống"),
                    "to", anonymize(toUser, "Hệ thống")
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Hàm ẩn danh tên: 
     * - Nếu là CITIZEN (người dân): Viết tắt tên hoặc đổi thành "Người dân nhận"
     * - Nếu là SHOP, DONOR, ADMIN: Giữ nguyên tên để công khai minh bạch
     */
    private String anonymize(User user, String defaultName) {
        if (user == null) return defaultName;
        if (user.getRole() == Role.CITIZEN) {
            String fullName = user.getFullName();
            if (fullName == null || fullName.isBlank()) return "Người dân";
            
            // Cách 1: Tên viết tắt (Nguyễn Văn A -> N. V. A)
            // Cách 2: Chọn hiển thị "Người nhận (Ẩn danh)" để tuyệt đối bảo mật
            // Theo yêu cầu của dự án: "Che giấu người dân"
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length > 0) {
                // Ví dụ hiển thị: Nguyễn V*** A***
                return parts[0] + " ***"; 
            }
            return "Người dân (Ẩn danh)";
        }
        return user.getFullName() != null ? user.getFullName() : defaultName;
    }
}
