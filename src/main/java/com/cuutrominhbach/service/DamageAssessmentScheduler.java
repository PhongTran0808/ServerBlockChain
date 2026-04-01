package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.entity.DamageAssessment;
import com.cuutrominhbach.entity.DamageAssessmentStatus;
import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.repository.DamageAssessmentRepository;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DamageAssessmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(DamageAssessmentScheduler.class);
    private static final BigInteger TOKEN_ID = BigInteger.ONE;

    private final DamageAssessmentRepository damageAssessmentRepository;
    private final BlockchainService blockchainService;
    private final TransactionHistoryRepository transactionHistoryRepository;
    
    @Value("${damage.relief.base-tokens:50}")
    private long baseTokens;

    public DamageAssessmentScheduler(DamageAssessmentRepository damageAssessmentRepository,
                                     BlockchainService blockchainService,
                                     TransactionHistoryRepository transactionHistoryRepository) {
        this.damageAssessmentRepository = damageAssessmentRepository;
        this.blockchainService = blockchainService;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    // Chạy mỗi giờ: 0 0 * * * *
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void approvePendingDamages() {
        log.info("Bắt đầu quét DamageAssessment đã qua 72h...");
        LocalDateTime targetTime = LocalDateTime.now().minusHours(72);

        List<DamageAssessment> pendingRecords = damageAssessmentRepository
                .findByStatusAndCreatedAtBefore(DamageAssessmentStatus.PENDING_3_DAYS, targetTime);

        if (pendingRecords.isEmpty()) {
            log.info("Không có hồ sơ khảo sát nào cần giải ngân.");
            return;
        }

        for (DamageAssessment record : pendingRecords) {
            try {
                // 1. Cập nhật status
                record.setStatus(DamageAssessmentStatus.APPROVED);
                record.setApprovedAt(LocalDateTime.now());
                damageAssessmentRepository.save(record);

                // Giải ngân tự động cấu hình 1 phần thưởng cơ bản (baseTokens) như 1 người
                // Mức 1 là x1, mức 2 là x2, mức 3 là x3 (như 3 người)
                // baseTokens có thể chỉnh trong application.properties
                long tokens = record.getDamageLevel() * baseTokens;

                if (tokens == 0) continue;

                // 3. Giải ngân bằng Web3
                String wallet = record.getCitizen().getWalletAddress();
                if (wallet != null && !wallet.isBlank()) {
                    String txHash = blockchainService.mintToken(wallet, TOKEN_ID, BigInteger.valueOf(tokens));
                    
                    // 4. Lưu lại lịch sử giao dịch
                    TransactionHistory history = new TransactionHistory(
                            null,
                            record.getCitizen().getId(),
                            TransactionHistory.TxType.AIRDROP, // Có thể cập nhật thành DAMAGE_RELIEF
                            tokens,
                            "Trợ cấp thiệt hại bão lụt (Mức " + record.getDamageLevel() + ")",
                            txHash
                    );
                    transactionHistoryRepository.save(history);
                    log.info("Đã giải ngân thành công hồ sơ #{} cho ví {}", record.getId(), wallet);
                } else {
                    log.warn("Citizen {} chưa gắn ví hợp lệ!", record.getCitizen().getId());
                }

            } catch (Exception e) {
                log.error("Lỗi giải ngân hồ sơ #{}: {}", record.getId(), e.getMessage());
                // Không throw rollback ở đây, tiếp tục xử lý các record khác
            }
        }
        log.info("Hoàn tất quét DamageAssessment.");
    }
}
