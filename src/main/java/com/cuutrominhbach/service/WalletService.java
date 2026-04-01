package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final TransactionHistoryRepository txRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlockchainService blockchainService;
    private final AirdropService airdropService;

    private static final BigInteger TOKEN_ID = BigInteger.ONE;

    public WalletService(UserRepository userRepository,
                         CampaignPoolRepository campaignPoolRepository,
                         TransactionHistoryRepository txRepository,
                         PasswordEncoder passwordEncoder,
                         BlockchainService blockchainService,
                         AirdropService airdropService) {
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.txRepository = txRepository;
        this.passwordEncoder = passwordEncoder;
        this.blockchainService = blockchainService;
        this.airdropService = airdropService;
    }

    // ── Top-up ────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> topUp(Long userId, Long amount, String pin) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(pin, user.getHashPassword()))
            throw new AuthException("Mã PIN không đúng");

        if (user.getWalletAddress() == null || user.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Người dùng chưa có ví blockchain");
        }

        String txHash = blockchainService.mintToken(
            user.getWalletAddress(),
            TOKEN_ID,
            BigInteger.valueOf(amount)
        );

        TransactionHistory tx = new TransactionHistory(
                null, userId, TransactionHistory.TxType.IN, amount,
            "Nạp tiền từ Ngân hàng", txHash
        );
        txRepository.save(tx);

        return Map.of("message", "Nạp tiền thành công", "amount", amount, "txHash", txHash);
    }

    // ── Donate ────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> donate(Long senderId, String province, Long amount, String pin) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(pin, sender.getHashPassword()))
            throw new AuthException("Mã PIN không đúng");

        // Kiểm tra pool có đang nhận không
        CampaignPool pool = campaignPoolRepository.findByProvince(province)
                .orElseThrow(() -> new IllegalArgumentException("Khu vực không tồn tại"));

        if (!Boolean.TRUE.equals(pool.getIsReceivingActive()))
            throw new IllegalArgumentException("Khu vực này đang tạm đóng nhận quyên góp");

        // 1. Cập nhật pool (Tiền Donate xong chỉ cộng vào CampaignPool)
        pool.setTotalFund(pool.getTotalFund() + amount);
        pool.setUpdatedAt(LocalDateTime.now());
        campaignPoolRepository.save(pool);

        // 2. Ghi log DONATE cho người gửi (Hào tâm → Quỹ Tỉnh)
        TransactionHistory tx = new TransactionHistory(
                senderId, null, TransactionHistory.TxType.DONATE, amount,
                "Quyên góp cho khu vực " + province, null
        );
        txRepository.save(tx);

        // 3. Nếu khu vực đã bật "Phân bổ quỹ", tự động phân chia tiền quyên góp cho người dân
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("message", "Quyên góp thành công");
        result.put("province", province);
        result.put("totalAmount", amount);
        
        if (Boolean.TRUE.equals(pool.getIsAutoAirdrop())) {
            try {
                String distributionMessage = airdropService.distributeRemainingFunds(pool.getId());
                result.put("autoDistributed", true);
                result.put("distributionMessage", distributionMessage);
            } catch (Exception ex) {
                // Log lỗi nhưng vẫn return thành công donate, distribution là bonus
                result.put("autoDistributed", false);
                result.put("distributionError", ex.getMessage());
            }
        } else {
            result.put("autoDistributed", false);
        }
        
        return result;
    }

    // ── Get Transactions ──────────────────────────────────────────────────────

    public List<TransactionHistory> getTransactions(Long userId) {
        return txRepository.findByUserId(userId);
    }

    // ── Withdraw (Mock) ───────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> withdraw(Long userId, Long amount, String pin) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Số token phải lớn hơn 0");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(pin, user.getHashPassword()))
            throw new AuthException("Mã PIN không đúng");

        // Ghi lịch sử rút tiền — Giai đoạn 3: Rút tiền mặt
        TransactionHistory tx = new TransactionHistory(
                userId, null, TransactionHistory.TxType.WITHDRAW, amount,
                "Quy đổi Token thành tiền mặt tái thiết sau bão",
                "mock-burn-" + System.currentTimeMillis()
        );
        txRepository.save(tx);

        long vnd = amount * 1000L; // 1 token = 1.000 VNĐ (mock rate)
        return Map.of(
                "message", "Yêu cầu rút tiền thành công",
                "tokenAmount", amount,
                "vndAmount", vnd
        );
    }

    // ── Pay Shop (Citizen scans QR) ───────────────────────────────────────────

    @Transactional
    public Map<String, Object> payShopDirect(Long citizenId, Long shopId, Long amount, String pin) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new AuthException("Người dân không tồn tại"));

        if (!passwordEncoder.matches(pin, citizen.getHashPassword()))
            throw new AuthException("Mã PIN không đúng");

        if (citizen.getRole() != Role.CITIZEN)
            throw new IllegalArgumentException("Chỉ người dân mới có thể thanh toán");

        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Cửa hàng không tồn tại"));

        if (shop.getRole() != Role.SHOP)
            throw new IllegalArgumentException("Mã QR không hợp lệ (Không phải của Shop)");

        if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank() ||
            shop.getWalletAddress() == null || shop.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Người dân hoặc Cửa hàng chưa có ví blockchain");
        }

        // Thực hiện chuyển on-chain
        // Nếu số dư không đủ, hàm này sẽ ném BlockchainException ("Giao dịch blockchain thất bại")
        String txHash = blockchainService.transferToken(
                citizen.getWalletAddress(),
                shop.getWalletAddress(),
                TOKEN_ID,
                BigInteger.valueOf(amount)
        );

        // Ghi transaction history
        TransactionHistory tx = new TransactionHistory(
                citizenId, shopId, TransactionHistory.TxType.PAY_SHOP, amount,
                "Thanh toán mua hàng tại " + (shop.getFullName() != null ? shop.getFullName() : shop.getUsername()),
                txHash
        );
        txRepository.save(tx);

        return Map.of(
                "message", "Thanh toán thành công",
                "amount", amount,
                "shopName", shop.getFullName() != null ? shop.getFullName() : shop.getUsername(),
                "txHash", txHash
        );
    }
}
