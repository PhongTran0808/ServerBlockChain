package com.cuutrominhbach.controller;

import com.cuutrominhbach.entity.ReliefBatchStatus;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.ReliefBatchRepository;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final UserRepository userRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final ReliefBatchRepository reliefBatchRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public AnalyticsController(UserRepository userRepository,
                                CampaignPoolRepository campaignPoolRepository,
                                ReliefBatchRepository reliefBatchRepository,
                                TransactionHistoryRepository transactionHistoryRepository) {
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.reliefBatchRepository = reliefBatchRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    /**
     * Phân bổ token: tổng quỹ, đã phân bổ (DELIVERED), đang khóa (IN_TRANSIT/PENDING)
     */
    @GetMapping("/token-flow")
    public ResponseEntity<Map<String, Object>> tokenFlow() {
        long totalFund = campaignPoolRepository.findAll()
                .stream().mapToLong(cp -> cp.getTotalFund() != null ? cp.getTotalFund() : 0L).sum();

        List<com.cuutrominhbach.entity.ReliefBatch> allBatches = reliefBatchRepository.findAll();

        long distributed = allBatches.stream()
                .filter(b -> b.getStatus() == ReliefBatchStatus.COMPLETED)
                .mapToLong(b -> b.getTokenPerPackage() != null && b.getTotalPackages() != null ? b.getTokenPerPackage() * b.getTotalPackages() : 0L).sum();

        long locked = allBatches.stream()
                .filter(b -> b.getStatus() != ReliefBatchStatus.COMPLETED)
                .mapToLong(b -> b.getTokenPerPackage() != null && b.getTotalPackages() != null ? b.getTokenPerPackage() * b.getTotalPackages() : 0L).sum();

        return ResponseEntity.ok(Map.of(
                "totalFund", totalFund,
                "distributed", distributed,
                "locked", locked,
                "available", Math.max(0, totalFund - distributed - locked)
        ));
    }

    /**
     * Thống kê theo ngày — số lô hàng cứu trợ theo trạng thái
     */
    @GetMapping("/daily-stats")
    public ResponseEntity<Map<String, Object>> dailyStats() {
        List<com.cuutrominhbach.entity.ReliefBatch> allBatches = reliefBatchRepository.findAll();
        
        long waiting = allBatches.stream().filter(b -> b.getStatus() == ReliefBatchStatus.CREATED || b.getStatus() == ReliefBatchStatus.WAITING_SHOP).count();
        long accepted = allBatches.stream().filter(b -> b.getStatus() == ReliefBatchStatus.ACCEPTED || b.getStatus() == ReliefBatchStatus.PICKED_UP).count();
        long inProgress = allBatches.stream().filter(b -> b.getStatus() == ReliefBatchStatus.IN_PROGRESS).count();
        long completed = allBatches.stream().filter(b -> b.getStatus() == ReliefBatchStatus.COMPLETED).count();

        return ResponseEntity.ok(Map.of(
                "waiting", waiting,
                "accepted", accepted,
                "inProgress", inProgress,
                "completed", completed,
                "totalBatches", allBatches.size()
        ));
    }

    /**
     * 10 giao dịch gần nhất từ blockchain ledger
     */
    @GetMapping("/live-feed")
    public ResponseEntity<List<Map<String, Object>>> liveFeed() {
        List<TransactionHistory> recentTxs = transactionHistoryRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(10)
                .collect(Collectors.toList());

        Map<Long, String> nameCache = new HashMap<>();
        for (TransactionHistory tx : recentTxs) {
            if (tx.getFromUserId() != null && !nameCache.containsKey(tx.getFromUserId())) {
                userRepository.findById(tx.getFromUserId()).ifPresent(u -> nameCache.put(u.getId(), u.getFullName()));
            }
            if (tx.getToUserId() != null && !nameCache.containsKey(tx.getToUserId())) {
                userRepository.findById(tx.getToUserId()).ifPresent(u -> nameCache.put(u.getId(), u.getFullName()));
            }
        }

        List<Map<String, Object>> feed = recentTxs.stream()
                .map(tx -> {
                    String fromName = tx.getFromUserId() != null ? nameCache.getOrDefault(tx.getFromUserId(), "Unknown") : "Hệ thống";
                    String toName = tx.getToUserId() != null ? nameCache.getOrDefault(tx.getToUserId(), "Unknown") : "Hệ thống";
                    
                    return Map.<String, Object>of(
                            "orderId", tx.getId(),
                            "citizen", fromName + " -> " + toName,
                            "status", tx.getType().name(),
                            "tokens", tx.getAmount() != null ? tx.getAmount() : 0,
                            "createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(feed);
    }
}
