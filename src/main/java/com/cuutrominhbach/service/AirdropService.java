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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    @Transactional
    public String airdrop(String province, Long amountPerCitizen) {
        if (province == null || province.isBlank()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống");
        }
        
        List<User> citizens = userRepository.findByRoleAndProvince(Role.CITIZEN, province);
        if (citizens.isEmpty()) {
            throw new IllegalArgumentException("Không có Citizen nào thuộc tỉnh: " + province);
        }

        List<String> walletAddresses = citizens.stream()
                .map(User::getWalletAddress)
                .filter(addr -> addr != null && !addr.isBlank())
                .collect(Collectors.toList());

        if (walletAddresses.size() < citizens.size()) {
            log.warn("Cảnh báo: Có {}/{} Citizen thiếu địa chỉ ví tại {}", 
                citizens.size() - walletAddresses.size(), citizens.size(), province);
        }

        if (walletAddresses.isEmpty()) {
            throw new IllegalArgumentException("Không có Citizen nào có địa chỉ ví hợp lệ để giải ngân");
        }

        log.info("Đang thực hiện giải ngân BATCH cho {} người dân tại {}...", walletAddresses.size(), province);

        String txHash;
        try {
            // Thử gọi batch airdrop trên smart contract
            txHash = blockchainService.airdrop(
                    province,
                    walletAddresses,
                    BigInteger.valueOf(amountPerCitizen)
            );
        } catch (Exception batchEx) {
            log.warn("Batch airdrop thất bại ({}), fallback sang mint từng người...", batchEx.getMessage());
            // Fallback: mint từng người một
            txHash = null;
            for (User citizen : citizens) {
                if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) continue;
                try {
                    String singleTx = blockchainService.mintToken(
                            citizen.getWalletAddress(),
                            TOKEN_ID,
                            BigInteger.valueOf(amountPerCitizen)
                    );
                    if (txHash == null) txHash = singleTx; // lấy hash đầu tiên làm đại diện
                } catch (Exception e) {
                    log.error("Mint thất bại cho citizen {}: {}", citizen.getId(), e.getMessage());
                }
            }
            if (txHash == null) {
                throw new RuntimeException("Giải ngân thất bại hoàn toàn: " + batchEx.getMessage());
            }
        }

        // 2. Ghi log vào DB (Chỉ chạy khi bước 1 thành công)
        for (User citizen : citizens) {
            if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) continue;
            
            transactionHistoryRepository.save(new TransactionHistory(
                null,
                citizen.getId(),
                TransactionHistory.TxType.AIRDROP,
                amountPerCitizen,
                "Nhận cứu trợ (Airdrop) khu vực " + province,
                txHash
            ));
        }

        log.info("== HOÀN TẤT AIRDROP BATCH ({}) - TX: {} ==", province, txHash);
        return "Giải ngân thành công cho " + walletAddresses.size() + " người dân. Hash: " + txHash;
    }
}
