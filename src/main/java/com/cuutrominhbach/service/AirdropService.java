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

@Service
public class AirdropService {

    private static final Logger log = LoggerFactory.getLogger(AirdropService.class);
    private static final BigInteger TOKEN_ID = BigInteger.ONE;

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
     * Airdrop tokens to all CITIZEN users in a province.
     * Returns list of transaction hashes.
     */
    public List<String> airdrop(String province, Long amountPerCitizen) {
        if (province == null || province.isBlank()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống");
        }
        
        if (!governmentAdministrativeService.isValidProvince(province.trim())) {
            throw new IllegalArgumentException("Tỉnh/Thành không hợp lệ theo dữ liệu địa giới hành chính chính phủ");
        }
        
        if (distributionRoundRepository.existsByProvince(province)) {
            throw new IllegalArgumentException("Đã tồn tại Merkle distribution round cho tỉnh này, không thể chạy airdrop legacy để tránh overlap");
        }

        List<User> citizens = userRepository.findByRoleAndProvince(Role.CITIZEN, province);
        if (citizens.isEmpty()) {
            throw new IllegalArgumentException("Không có Citizen nào thuộc tỉnh: " + province);
        }

        List<String> txHashes = new ArrayList<>();
        for (User citizen : citizens) {
            if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) {
                log.warn("Citizen {} không có wallet address, bỏ qua", citizen.getId());
                continue;
            }
            try {
                String txHash = blockchainService.mintToken(
                        citizen.getWalletAddress(),
                        TOKEN_ID,
                        BigInteger.valueOf(amountPerCitizen)
                );
                txHashes.add(txHash);

                transactionHistoryRepository.save(new TransactionHistory(
                    null,
                    citizen.getId(),
                    TransactionHistory.TxType.IN,
                    amountPerCitizen,
                    "Nhận cứu trợ campaign " + province,
                    txHash
                ));
            } catch (Exception e) {
                log.error("Airdrop thất bại cho citizen {}: {}", citizen.getId(), e.getMessage());
            }
        }
        return txHashes;
    }
}
