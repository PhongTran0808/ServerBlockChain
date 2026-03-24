package com.cuutrominhbach.controller;

import com.cuutrominhbach.entity.OrderStatus;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.OrderRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CampaignPoolRepository campaignPoolRepository;

    public AnalyticsController(OrderRepository orderRepository,
                                UserRepository userRepository,
                                CampaignPoolRepository campaignPoolRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
    }

    /**
     * Phân bổ token: tổng quỹ, đã phân bổ (DELIVERED), đang khóa (IN_TRANSIT/PENDING)
     */
    @GetMapping("/token-flow")
    public ResponseEntity<Map<String, Object>> tokenFlow() {
        long totalFund = campaignPoolRepository.findAll()
                .stream().mapToLong(cp -> cp.getTotalFund() != null ? cp.getTotalFund() : 0L).sum();

        long distributed = orderRepository.findByStatus(OrderStatus.DELIVERED)
                .stream().mapToLong(o -> o.getTotalTokens() != null ? o.getTotalTokens() : 0L).sum();

        long locked = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING
                        || o.getStatus() == OrderStatus.READY
                        || o.getStatus() == OrderStatus.IN_TRANSIT)
                .mapToLong(o -> o.getTotalTokens() != null ? o.getTotalTokens() : 0L).sum();

        return ResponseEntity.ok(Map.of(
                "totalFund", totalFund,
                "distributed", distributed,
                "locked", locked,
                "available", Math.max(0, totalFund - distributed - locked)
        ));
    }

    /**
     * Thống kê theo ngày — số đơn hàng theo trạng thái
     */
    @GetMapping("/daily-stats")
    public ResponseEntity<Map<String, Object>> dailyStats() {
        long pending = orderRepository.findByStatus(OrderStatus.PENDING).size();
        long inTransit = orderRepository.findByStatus(OrderStatus.IN_TRANSIT).size();
        long delivered = orderRepository.findByStatus(OrderStatus.DELIVERED).size();
        long cancelled = orderRepository.findByStatus(OrderStatus.CANCELLED).size();

        return ResponseEntity.ok(Map.of(
                "pending", pending,
                "inTransit", inTransit,
                "delivered", delivered,
                "cancelled", cancelled,
                "totalOrders", pending + inTransit + delivered + cancelled
        ));
    }

    /**
     * 10 giao dịch gần nhất
     */
    @GetMapping("/live-feed")
    public ResponseEntity<List<Map<String, Object>>> liveFeed() {
        List<Map<String, Object>> feed = orderRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(10)
                .map(order -> {
                    String citizenName = order.getCitizen() != null ? order.getCitizen().getFullName() : "N/A";
                    return Map.<String, Object>of(
                            "orderId", order.getId(),
                            "citizen", citizenName,
                            "status", order.getStatus().name(),
                            "tokens", order.getTotalTokens() != null ? order.getTotalTokens() : 0,
                            "createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(feed);
    }
}
