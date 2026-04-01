package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.CreateCampaignRequest;
import com.cuutrominhbach.dto.request.CreateItemRequest;
import com.cuutrominhbach.dto.request.ReviewUserRequest;
import com.cuutrominhbach.dto.response.*;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.repository.*;
import com.cuutrominhbach.service.AirdropService;
import com.cuutrominhbach.service.GovernmentAdministrativeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final OrderRepository orderRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AirdropService airdropService;
    private final GovernmentAdministrativeService governmentAdministrativeService;

    public AdminController(UserRepository userRepository,
                           ItemRepository itemRepository,
                           CampaignPoolRepository campaignPoolRepository,
                           OrderRepository orderRepository,
                           TransactionHistoryRepository transactionHistoryRepository,
                           AirdropService airdropService,
                           GovernmentAdministrativeService governmentAdministrativeService) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.orderRepository = orderRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.airdropService = airdropService;
        this.governmentAdministrativeService = governmentAdministrativeService;
    }

    // ── Debug ────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User current = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        return ResponseEntity.ok(Map.of(
                "userId", current.getId(),
                "username", current.getUsername(),
                "fullName", current.getFullName(),
                "role", current.getRole().name(),
                "isAdmin", current.getRole() == Role.ADMIN,
                "isApproved", current.getIsApproved(),
                "province", current.getProvince() == null ? "" : current.getProvince()
        ));
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        ensureAdmin();

        long totalFund = campaignPoolRepository.findAll()
                .stream().mapToLong(cp -> cp.getTotalFund() != null ? cp.getTotalFund() : 0L).sum();
        long totalCitizens = userRepository.countByRole(Role.CITIZEN);
        long approvedShops = userRepository.countByRoleAndIsApproved(Role.SHOP, true);
        long totalAirdrops = orderRepository.count();

        return ResponseEntity.ok(new AdminStatsResponse(totalFund, totalCitizens, approvedShops, totalAirdrops));
    }

    @GetMapping("/overview-stats")
    public ResponseEntity<Map<String, Object>> getOverviewStats() {
        ensureAdmin();

        long totalFunds = campaignPoolRepository.findAll().stream()
                .mapToLong(cp -> cp.getTotalFund() != null ? cp.getTotalFund() : 0L).sum();
        long totalCitizens = userRepository.countByRole(Role.CITIZEN);
        long totalShops = userRepository.countByRole(Role.SHOP);
        
        // Count DONORS as users who have DONATE transactions
        long totalDonors = transactionHistoryRepository.findAll().stream()
                .filter(tx -> tx.getType() == TransactionHistory.TxType.DONATE)
                .map(TransactionHistory::getFromUserId)
                .distinct()
                .count();

        List<TransactionHistory> recentTxs = transactionHistoryRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList());

        // Cache names for the 5 transactions
        Map<Long, String> nameCache = new HashMap<>();
        for (TransactionHistory tx : recentTxs) {
            if (tx.getFromUserId() != null && !nameCache.containsKey(tx.getFromUserId())) {
                userRepository.findById(tx.getFromUserId()).ifPresent(u -> nameCache.put(u.getId(), u.getFullName()));
            }
            if (tx.getToUserId() != null && !nameCache.containsKey(tx.getToUserId())) {
                userRepository.findById(tx.getToUserId()).ifPresent(u -> nameCache.put(u.getId(), u.getFullName()));
            }
        }

        List<Map<String, Object>> recentTxList = recentTxs.stream().map(tx -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", tx.getId());
            map.put("type", tx.getType().name());
            map.put("amount", tx.getAmount());
            map.put("note", tx.getNote());
            map.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "");
            map.put("txHash", tx.getTxHash() != null ? tx.getTxHash() : "");
            map.put("fromName", tx.getFromUserId() != null ? nameCache.getOrDefault(tx.getFromUserId(), "Unknown") : "Hệ thống");
            map.put("toName", tx.getToUserId() != null ? nameCache.getOrDefault(tx.getToUserId(), "Unknown") : "Hệ thống");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "totalFunds", totalFunds,
                "totalCitizens", totalCitizens,
                "totalShops", totalShops,
                "totalDonors", totalDonors,
                "recentTransactions", recentTxList
        ));
    }

    // ── Users ──────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(required = false) String q) {
        ensureAdmin();

        List<User> users = userRepository.findAll();
        if (q != null && !q.isBlank()) {
            String lower = q.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(lower))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(users.stream().map(UserResponse::from).collect(Collectors.toList()));
    }

    @PutMapping("/users/{id}/review")
    public ResponseEntity<UserResponse> reviewUser(@PathVariable Long id,
                                                   @RequestBody ReviewUserRequest request) {
        ensureAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String action = request != null && request.action() != null
                ? request.action().trim().toUpperCase()
                : "";

        switch (action) {
            case "APPROVE" -> user.setIsApproved(true);
            case "REJECT" -> user.setIsApproved(false);
            default -> throw new IllegalArgumentException("Action không hợp lệ. Chỉ chấp nhận APPROVE hoặc REJECT");
        }

        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    // Kept for backward compatibility with existing clients.
    @PutMapping("/users/{id}/approve")
    public ResponseEntity<UserResponse> toggleApprove(@PathVariable Long id) {
        ensureAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        user.setIsApproved(!Boolean.TRUE.equals(user.getIsApproved()));
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    // ── Items ──────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getItems() {
        ensureAdmin();

        return ResponseEntity.ok(
                itemRepository.findAll().stream().map(ItemResponse::from).collect(Collectors.toList())
        );
    }

    /** Endpoint public — citizen/shop đều gọi được để xem danh mục */
    @GetMapping("/items/public")
    public ResponseEntity<List<ItemResponse>> getPublicItems() {
        return ResponseEntity.ok(
                itemRepository.findByStatus(ItemStatus.ACTIVE)
                        .stream().map(ItemResponse::from).collect(Collectors.toList())
        );
    }

    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(@RequestBody CreateItemRequest req) {
        ensureAdmin();

        Item item = Item.builder()
                .tokenId(req.getTokenId())
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .status(ItemStatus.ACTIVE)
                .priceTokens(req.getPriceTokens())
                .build();
        return ResponseEntity.ok(ItemResponse.from(itemRepository.save(item)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id,
                                                    @RequestBody CreateItemRequest req) {
        ensureAdmin();

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vật phẩm"));
        if (req.getName() != null) item.setName(req.getName());
        if (req.getImageUrl() != null) item.setImageUrl(req.getImageUrl());
        if (req.getTokenId() != null) item.setTokenId(req.getTokenId());
        if (req.getPriceTokens() != null) item.setPriceTokens(req.getPriceTokens());
        return ResponseEntity.ok(ItemResponse.from(itemRepository.save(item)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        ensureAdmin();

        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Campaigns ─────────────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignPool>> getCampaigns() {
        ensureAdmin();

        return ResponseEntity.ok(campaignPoolRepository.findAll());
    }

    /** Endpoint public — Citizen gọi để lấy danh sách tỉnh đang nhận quyên góp */
    @GetMapping("/campaigns/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveCampaigns() {
        List<Map<String, Object>> result = campaignPoolRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsReceivingActive()))
                .map(p -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("province", p.getProvince());
                    m.put("totalFund", p.getTotalFund());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/campaigns/province-stats")
    public ResponseEntity<List<Map<String, Object>>> getCampaignProvinceStats() {
        ensureAdmin();

        Map<Long, User> userById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<String, Long> distributedByProvince = new HashMap<>();
        for (TransactionHistory tx : transactionHistoryRepository.findAll()) {
            if (tx.getType() != TransactionHistory.TxType.IN || tx.getToUserId() == null || tx.getAmount() == null) {
                continue;
            }

            User receiver = userById.get(tx.getToUserId());
            if (receiver == null || receiver.getRole() != Role.CITIZEN || receiver.getProvince() == null) {
                continue;
            }

            String note = tx.getNote() == null ? "" : tx.getNote();
            boolean isDistribution = note.startsWith("Nhận quyên góp") || note.startsWith("Nhận cứu trợ");
            if (!isDistribution) {
                continue;
            }

            distributedByProvince.merge(receiver.getProvince(), tx.getAmount(), Long::sum);
        }

        List<Map<String, Object>> stats = campaignPoolRepository.findAll().stream().map(pool -> {
            long totalFund = pool.getTotalFund() != null ? pool.getTotalFund() : 0L;
            long totalDistributed = distributedByProvince.getOrDefault(pool.getProvince(), 0L);
            long remaining = Math.max(0L, totalFund - totalDistributed);

            Map<String, Object> row = new HashMap<>();
            row.put("id", pool.getId());
            row.put("campaignCode", pool.getCampaignCode() == null || pool.getCampaignCode().isBlank()
                ? "CP-" + pool.getId()
                : pool.getCampaignCode());
            row.put("province", pool.getProvince() == null ? "" : pool.getProvince());
            row.put("totalFund", totalFund);
            row.put("totalDistributed", totalDistributed);
            row.put("remaining", remaining);
            row.put("isReceivingActive", Boolean.TRUE.equals(pool.getIsReceivingActive()));
            row.put("isAutoAirdrop", Boolean.TRUE.equals(pool.getIsAutoAirdrop()));
            row.put("updatedAt", pool.getUpdatedAt() != null ? pool.getUpdatedAt().toString() : "");
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/province-stats")
    public ResponseEntity<List<Map<String, Object>>> getProvinceStatsAlias() {
        ensureAdmin();
        return getCampaignProvinceStats();
    }

    @PostMapping("/campaigns")
    public ResponseEntity<CampaignPool> createCampaign(@RequestBody CreateCampaignRequest req) {
        ensureAdmin();

        if (req == null || req.province() == null || req.province().isBlank()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống");
        }

        String province = req.province().trim();
        if (!governmentAdministrativeService.isValidProvince(province)) {
            throw new IllegalArgumentException("Tỉnh/Thành không hợp lệ theo dữ liệu địa giới hành chính chính phủ mới nhất");
        }

        if (campaignPoolRepository.findByProvince(province).isPresent()) {
            throw new IllegalArgumentException("Campaign cho tỉnh này đã tồn tại");
        }

        String code = (req.campaignCode() == null || req.campaignCode().isBlank())
                ? "CP-" + province.toUpperCase().replace(" ", "-")
                : req.campaignCode().trim().toUpperCase();

        CampaignPool pool = CampaignPool.builder()
                .campaignCode(code)
                .province(province)
                .totalFund(0L)
                .isReceivingActive(true)
                .updatedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(campaignPoolRepository.save(pool));
    }

    @PutMapping("/campaigns/{id}/toggle")
    public ResponseEntity<CampaignPool> toggleCampaign(@PathVariable Long id) {
        ensureAdmin();

        CampaignPool pool = campaignPoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khu vực"));
        pool.setIsReceivingActive(!Boolean.TRUE.equals(pool.getIsReceivingActive()));
        return ResponseEntity.ok(campaignPoolRepository.save(pool));
    }

    @PutMapping("/campaigns/{id}/distribute-funds")
    public ResponseEntity<Map<String, String>> distributeFunds(@PathVariable Long id) {
        ensureAdmin();
        String resultMessage = airdropService.distributeRemainingFunds(id);
        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    // ── Airdrop ───────────────────────────────────────────────────────────────

    @PostMapping("/airdrop")
    public ResponseEntity<Map<String, Object>> airdrop(@RequestBody Map<String, Object> body) {
        ensureAdmin();

        String province = (String) body.get("province");
        Long amountPerCitizen = Long.valueOf(body.get("amountPerCitizen").toString());
        String msg = airdropService.airdrop(province, amountPerCitizen);
        return ResponseEntity.ok(Map.of(
                "province", province,
                "amountPerCitizen", amountPerCitizen,
                "message", msg
        ));
    }

    private void ensureAdmin() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User current = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền ADMIN"));

        if (current.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền ADMIN");
        }
    }
}
