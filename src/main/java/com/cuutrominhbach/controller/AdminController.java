package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.CreateItemRequest;
import com.cuutrominhbach.dto.response.*;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.repository.*;
import com.cuutrominhbach.service.AirdropService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AirdropService airdropService;

    public AdminController(UserRepository userRepository,
                           ItemRepository itemRepository,
                           CampaignPoolRepository campaignPoolRepository,
                           OrderRepository orderRepository,
                           AirdropService airdropService) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.orderRepository = orderRepository;
        this.airdropService = airdropService;
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        long totalFund = campaignPoolRepository.findAll()
                .stream().mapToLong(cp -> cp.getTotalFund() != null ? cp.getTotalFund() : 0L).sum();
        long totalCitizens = userRepository.countByRole(Role.CITIZEN);
        long approvedShops = userRepository.countByRoleAndIsApproved(Role.SHOP, true);
        long totalAirdrops = orderRepository.count();

        return ResponseEntity.ok(new AdminStatsResponse(totalFund, totalCitizens, approvedShops, totalAirdrops));
    }

    // ── Users ──────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream().map(UserResponse::from).collect(Collectors.toList())
        );
    }

    @PutMapping("/users/{id}/approve")
    public ResponseEntity<UserResponse> toggleApprove(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        user.setIsApproved(!Boolean.TRUE.equals(user.getIsApproved()));
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    // ── Items ──────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getItems() {
        return ResponseEntity.ok(
                itemRepository.findAll().stream().map(ItemResponse::from).collect(Collectors.toList())
        );
    }

    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(@RequestBody CreateItemRequest req) {
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
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Campaigns ─────────────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignPool>> getCampaigns() {
        return ResponseEntity.ok(campaignPoolRepository.findAll());
    }

    @PutMapping("/campaigns/{id}/toggle")
    public ResponseEntity<CampaignPool> toggleCampaign(@PathVariable Long id) {
        CampaignPool pool = campaignPoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khu vực"));
        pool.setIsReceivingActive(!Boolean.TRUE.equals(pool.getIsReceivingActive()));
        return ResponseEntity.ok(campaignPoolRepository.save(pool));
    }

    // ── Airdrop ───────────────────────────────────────────────────────────────

    @PostMapping("/airdrop")
    public ResponseEntity<Map<String, Object>> airdrop(@RequestBody Map<String, Object> body) {
        String province = (String) body.get("province");
        Long amountPerCitizen = Long.valueOf(body.get("amountPerCitizen").toString());
        List<String> txHashes = airdropService.airdrop(province, amountPerCitizen);
        return ResponseEntity.ok(Map.of(
                "province", province,
                "amountPerCitizen", amountPerCitizen,
                "txHashes", txHashes,
                "count", txHashes.size()
        ));
    }
}
