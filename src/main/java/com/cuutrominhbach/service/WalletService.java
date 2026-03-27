package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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

    private static final BigInteger TOKEN_ID = BigInteger.ONE;

    public WalletService(UserRepository userRepository,
                         CampaignPoolRepository campaignPoolRepository,
                         TransactionHistoryRepository txRepository,
                         PasswordEncoder passwordEncoder,
                         BlockchainService blockchainService) {
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.txRepository = txRepository;
        this.passwordEncoder = passwordEncoder;
        this.blockchainService = blockchainService;
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

        // Lấy danh sách CITIZEN thuộc khu vực
        List<User> recipients = userRepository.findByRoleAndProvince(Role.CITIZEN, province);
        if (recipients.isEmpty())
            throw new IllegalArgumentException("Không có người dân nào trong khu vực này");

        long perPerson = amount / recipients.size();
        if (perPerson <= 0) throw new IllegalArgumentException("Số tiền quá nhỏ để chia đều");

        String anchorPayload = senderId + ":" + province + ":" + amount + ":" + recipients.size() + ":" + System.currentTimeMillis();
        String anchorRoot = Numeric.toHexString(Hash.sha3(anchorPayload.getBytes(StandardCharsets.UTF_8)));
        String txHash = blockchainService.storeMerkleRoot(anchorRoot);

        // Ghi OUT cho người gửi
        txRepository.save(new TransactionHistory(
                senderId, null, TransactionHistory.TxType.OUT, amount,
            "Quyên góp cho khu vực " + province, txHash
        ));

        // Ghi IN cho từng người nhận
        for (User recipient : recipients) {
            txRepository.save(new TransactionHistory(
                    senderId, recipient.getId(), TransactionHistory.TxType.IN, perPerson,
                    "Nhận quyên góp từ " + sender.getFullName(), txHash
            ));
        }

        // Cập nhật pool
        pool.setTotalFund(pool.getTotalFund() + amount);
        pool.setUpdatedAt(LocalDateTime.now());
        campaignPoolRepository.save(pool);

        return Map.of(
                "message", "Quyên góp thành công",
                "province", province,
                "totalAmount", amount,
                "recipients", recipients.size(),
                "perPerson", perPerson,
                "txHash", txHash
        );
    }

    // ── Get Transactions ──────────────────────────────────────────────────────

    public List<TransactionHistory> getTransactions(Long userId) {
        return txRepository.findByUserId(userId);
    }
}
