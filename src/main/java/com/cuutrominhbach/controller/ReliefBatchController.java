package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.ReliefBatchResponse;
import com.cuutrominhbach.security.JwtTokenProvider;
import com.cuutrominhbach.service.ReliefBatchService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class ReliefBatchController {

    private final ReliefBatchService batchService;
    private final JwtTokenProvider jwtTokenProvider;

    public ReliefBatchController(ReliefBatchService batchService, JwtTokenProvider jwtTokenProvider) {
        this.batchService = batchService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    /** POST /api/batches — Admin tạo lô cứu trợ */
    @PostMapping
    public ResponseEntity<ReliefBatchResponse> createBatch(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        Long adminId = getUserId(request);
        return ResponseEntity.ok(batchService.createBatch(adminId, body));
    }

    /** GET /api/batches/all — Admin xem tất cả lô */
    @GetMapping("/all")
    public ResponseEntity<List<ReliefBatchResponse>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    /** GET /api/provinces/stats?province=... — Thống kê tỉnh (citizens + tokens) */
    @GetMapping("/province-stats")
    public ResponseEntity<java.util.Map<String, Object>> getProvinceStats(
            @RequestParam String province) {
        return ResponseEntity.ok(batchService.getProvinceStats(province));
    }

    // ── TNV ───────────────────────────────────────────────────────────────────

    /** GET /api/batches/available?province=... — TNV xem lô CREATED */
    @GetMapping("/available")
    public ResponseEntity<List<ReliefBatchResponse>> getAvailable(
            @RequestParam(required = false) String province) {
        return ResponseEntity.ok(batchService.getAvailableBatches(province));
    }

    /** GET /api/batches/mine — TNV xem lô của mình */
    @GetMapping("/mine")
    public ResponseEntity<List<ReliefBatchResponse>> getMyBatches(HttpServletRequest request) {
        Long transporterId = getUserId(request);
        return ResponseEntity.ok(batchService.getMyBatches(transporterId));
    }

    /** POST /api/batches/{id}/claim — TNV nhận lô + chọn shop */
    @PostMapping("/{id}/claim")
    public ResponseEntity<ReliefBatchResponse> claimBatch(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        Long transporterId = getUserId(request);
        Long shopId = Long.parseLong(body.get("shopId").toString());
        return ResponseEntity.ok(batchService.claimBatch(id, transporterId, shopId));
    }

    /** POST /api/batches/{id}/pickup — TNV quét QR shop lấy hàng */
    @PostMapping("/{id}/pickup")
    public ResponseEntity<ReliefBatchResponse> pickupBatch(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        Long transporterId = getUserId(request);
        String qrData = (String) body.get("qrData");
        return ResponseEntity.ok(batchService.pickupBatch(id, transporterId, qrData));
    }

    /** POST /api/batches/{id}/deliver — TNV quét QR citizen phân phát 1 phần */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<ReliefBatchResponse> deliverToOneCitizen(@PathVariable Long id,
                                                                     @RequestBody Map<String, Object> body,
                                                                     HttpServletRequest request) {
        Long transporterId = getUserId(request);
        String citizenWallet = body != null ? (String) body.get("citizenWallet") : null;
        
        if (citizenWallet == null || citizenWallet.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(batchService.deliverToOneCitizen(id, transporterId, citizenWallet));
    }

    // ── SHOP ──────────────────────────────────────────────────────────────────

    /** GET /api/batches/shop/pending — Shop xem lô đang chờ duyệt */
    @GetMapping("/shop/pending")
    public ResponseEntity<List<ReliefBatchResponse>> getShopPending(HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(batchService.getPendingBatchesForShop(shopId));
    }

    /** GET /api/batches/shop/all — Shop xem tất cả lô của mình */
    @GetMapping("/shop/all")
    public ResponseEntity<List<ReliefBatchResponse>> getShopAll(HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(batchService.getAllBatchesForShop(shopId));
    }

    /** POST /api/batches/{id}/accept — Shop chấp nhận lô */
    @PostMapping("/{id}/accept")
    public ResponseEntity<ReliefBatchResponse> acceptBatch(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(batchService.acceptBatch(id, shopId));
    }

    /** POST /api/batches/{id}/reject — Shop từ chối lô */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ReliefBatchResponse> rejectBatch(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(batchService.rejectBatch(id, shopId));
    }

    /** GET /api/batches/{id} — Xem chi tiết 1 lô theo ID */
    @GetMapping("/{id}")
    public ResponseEntity<ReliefBatchResponse> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    /** DELETE /api/batches/{id} — Admin xóa lô (chỉ khi CREATED) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getUserId(request);
        batchService.deleteBatch(id, adminId);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/batches/{id}/return — TNV trả lô về Shop khi dân không đến nhận đủ */
    @PostMapping("/{id}/return")
    public ResponseEntity<ReliefBatchResponse> returnBatch(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Long transporterId = getUserId(request);
        return ResponseEntity.ok(batchService.returnBatchToShop(id, transporterId));
    }

    /** GET /api/batches/{id}/transactions — Xem tất cả giao dịch của 1 lô (truy vết) */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<com.cuutrominhbach.dto.response.TransactionResponse>> getBatchTransactions(
            @PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchTransactions(id));
    }

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header.substring(7);
        Claims claims = jwtTokenProvider.parseToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }
}
