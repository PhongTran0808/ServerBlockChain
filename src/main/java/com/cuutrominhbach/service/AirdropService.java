package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.DistributionRoundRepository;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AirdropService {

    private static final Logger log = LoggerFactory.getLogger(AirdropService.class);
    private static final BigInteger TOKEN_ID = BigInteger.ONE;
    private final ExecutorService airdropExecutor = Executors.newSingleThreadExecutor();

    private final UserRepository userRepository;
    private final BlockchainService blockchainService;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final DistributionRoundRepository distributionRoundRepository;
    private final GovernmentAdministrativeService governmentAdministrativeService;

    public AirdropService(UserRepository userRepository,
                          BlockchainService blockchainService,
                          TransactionHistoryRepository transactionHistoryRepository,
                          DistributionRoundRepository distributionRoundRepository,
                          GovernmentAdministrativeService governmentAdministrativeService) {
        this.userRepository = userRepository;
        this.blockchainService = blockchainService;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.distributionRoundRepository = distributionRoundRepository;
        this.governmentAdministrativeService = governmentAdministrativeService;
    }

    /**
     * Airdrop tokens to all CITIZEN users in a province via a background queue.
     * Guaranteed sequential execution to prevent Web3 RPC Rate Limit & Nonce collision.
     */
    public String airdrop(String province, Long amountPerCitizen) {
        if (province == null || province.isBlank()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống");
        }
        
        if (distributionRoundRepository.existsByProvince(province)) {
            throw new IllegalArgumentException("Đã tồn tại Merkle distribution round cho tỉnh này, không thể chạy airdrop legacy để tránh overlap");
        }

        List<User> citizens = userRepository.findByRoleAndProvince(Role.CITIZEN, province);
        if (citizens.isEmpty()) {
            throw new IllegalArgumentException("Không có Citizen nào thuộc tỉnh: " + province);
        }

        log.info("Khởi động Airdrop cho {} người dân tại {}. Xin vui lòng chờ...", citizens.size(), province);

        airdropExecutor.submit(() -> {
            int successCount = 0;
            log.info("== BẮT ĐẦU VÒNG LẶP AIRDROP NGẦM ({}) ==", province);

            for (User citizen : citizens) {
                if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) {
                    log.warn("Citizen {} không có wallet address, bỏ qua", citizen.getId());
                    continue;
                }
                try {
                    // Call RPC (FastRawTransactionManager ensures correct nonce within a single thread)
                    String txHash = blockchainService.mintToken(
                            citizen.getWalletAddress(),
                            TOKEN_ID,
                            BigInteger.valueOf(amountPerCitizen)
                    );

                    transactionHistoryRepository.save(new TransactionHistory(
                        null,
                        citizen.getId(),
                        TransactionHistory.TxType.AIRDROP,
                        amountPerCitizen,
                        "Nhận cứu trợ (Airdrop) khu vực " + province,
                        txHash
                    ));
                    successCount++;
                    
                    // Delay 2000ms to avoid slamming RPC and hitting rate limit (e.g. 429 Too Many Requests)
                    Thread.sleep(2000); 
                } catch (Exception e) {
                    log.error("Airdrop thất bại cho citizen {}: {}", citizen.getId(), e.getMessage());
                }
            }
            log.info("== HOÀN TẤT AIRDROP ({}) - Thành công {}/{} ==", province, successCount, citizens.size());
        });

        return "Đã đưa lệnh Airdrop cho " + citizens.size() + " người dân tại " + province + " vào hàng đợi hệ thống!";
    }
}
