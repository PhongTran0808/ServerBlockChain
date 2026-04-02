package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.request.RecoverWalletItem;
import com.cuutrominhbach.dto.request.RecoverWalletRequest;
import com.cuutrominhbach.dto.response.RecoverWalletResponse;
import com.cuutrominhbach.entity.ReliefBatch;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.ReliefBatchRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class WalletRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(WalletRecoveryService.class);
    private static final Pattern WALLET_REGEX = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final ReliefBatchRepository reliefBatchRepository;
    private final UserRepository userRepository;

    public WalletRecoveryService(ReliefBatchRepository reliefBatchRepository,
                                  UserRepository userRepository) {
        this.reliefBatchRepository = reliefBatchRepository;
        this.userRepository = userRepository;
    }

    public RecoverWalletResponse recover(RecoverWalletRequest request) {
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (RecoverWalletItem item : request.items()) {
            // 1. Validate walletAddress regex
            if (!WALLET_REGEX.matcher(item.walletAddress()).matches()) {
                String reason = String.format("batchId=%d, role=%s: walletAddress '%s' không hợp lệ (không khớp regex ^0x[a-fA-F0-9]{40}$)",
                        item.batchId(), item.role(), item.walletAddress());
                logger.warn(reason);
                skippedReasons.add(reason);
                skippedCount++;
                continue;
            }

            // 2. Find ReliefBatch by batchId
            Optional<ReliefBatch> batchOpt = reliefBatchRepository.findById(item.batchId());
            if (batchOpt.isEmpty()) {
                String reason = String.format("batchId=%d, role=%s: không tìm thấy ReliefBatch",
                        item.batchId(), item.role());
                logger.warn(reason);
                skippedReasons.add(reason);
                skippedCount++;
                continue;
            }

            ReliefBatch batch = batchOpt.get();
            User user;

            // 3 & 4. Get user by role
            if ("TRANSPORTER".equalsIgnoreCase(item.role())) {
                user = batch.getTransporter();
                if (user == null) {
                    String reason = String.format("batchId=%d, role=TRANSPORTER: transporter là null",
                            item.batchId());
                    logger.warn(reason);
                    skippedReasons.add(reason);
                    skippedCount++;
                    continue;
                }
            } else if ("SHOP".equalsIgnoreCase(item.role())) {
                user = batch.getShop();
                if (user == null) {
                    String reason = String.format("batchId=%d, role=SHOP: shop là null",
                            item.batchId());
                    logger.warn(reason);
                    skippedReasons.add(reason);
                    skippedCount++;
                    continue;
                }
            } else {
                String reason = String.format("batchId=%d: role '%s' không hợp lệ (chỉ chấp nhận TRANSPORTER hoặc SHOP)",
                        item.batchId(), item.role());
                logger.warn(reason);
                skippedReasons.add(reason);
                skippedCount++;
                continue;
            }

            // 5 & 6. Set walletAddress (lowercase) and save
            user.setWalletAddress(item.walletAddress().toLowerCase());
            userRepository.save(user);
            logger.info("Đã cập nhật walletAddress cho userId={}, batchId={}, role={}",
                    user.getId(), item.batchId(), item.role());
            updatedCount++;
        }

        return new RecoverWalletResponse(updatedCount, skippedCount, skippedReasons);
    }
}
