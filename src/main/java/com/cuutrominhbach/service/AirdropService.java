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
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.DamageAssessmentRepository;
import com.cuutrominhbach.entity.CampaignPool;
import com.cuutrominhbach.entity.DamageAssessment;
import com.cuutrominhbach.entity.DamageAssessmentStatus;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
    private final CampaignPoolRepository campaignPoolRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;

    public AirdropService(UserRepository userRepository,
                          BlockchainService blockchainService,
                          TransactionHistoryRepository transactionHistoryRepository,
                          DistributionRoundRepository distributionRoundRepository,
                          GovernmentAdministrativeService governmentAdministrativeService,
                          CampaignPoolRepository campaignPoolRepository,
                          DamageAssessmentRepository damageAssessmentRepository) {
        this.userRepository = userRepository;
        this.blockchainService = blockchainService;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.distributionRoundRepository = distributionRoundRepository;
        this.governmentAdministrativeService = governmentAdministrativeService;
        this.campaignPoolRepository = campaignPoolRepository;
        this.damageAssessmentRepository = damageAssessmentRepository;
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

    @Transactional
    public String distributeRemainingFunds(Long campaignPoolId) {
        CampaignPool pool = campaignPoolRepository.findById(campaignPoolId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch/khu vực"));
        
        String province = pool.getProvince();
        
        // 1. Calculate remaining funds
        Long totalFund = pool.getTotalFund() != null ? pool.getTotalFund() : 0L;
        Long totalDistributed = 0L;
        
        // Cần lấy tất cả giao dịch IN vào user có role là CITIZEN của province
        List<User> provinceCitizensList = userRepository.findByRoleAndProvince(Role.CITIZEN, province);
        if (provinceCitizensList.isEmpty()) {
            throw new IllegalArgumentException("Không có người dân nào tại khu vực này để giải ngân.");
        }
        
        Map<Long, User> citizenMap = provinceCitizensList.stream()
            .collect(Collectors.toMap(User::getId, u -> u));
            
        for (TransactionHistory tx : transactionHistoryRepository.findAll()) {
            if (tx.getType() == TransactionHistory.TxType.IN 
                && tx.getToUserId() != null 
                && citizenMap.containsKey(tx.getToUserId())) {
                String note = tx.getNote() != null ? tx.getNote() : "";
                if (note.startsWith("Nhận quyên góp") || note.startsWith("Nhận cứu trợ")) {
                    totalDistributed += tx.getAmount();
                }
            }
        }
        
        long remainingFunds = Math.max(0L, totalFund - totalDistributed);
        if (remainingFunds <= 0) {
            throw new IllegalArgumentException("Khởi tạo phân chia thất bại: Quỹ của khu vực này đã hết sơ với số đã giải ngân (" + totalDistributed + " / " + totalFund + ")");
        }

        // 2. Determine multipliers per citizen based on APPROVED DamageAssessment
        List<DamageAssessment> approvedAssessments = damageAssessmentRepository.findByStatus(DamageAssessmentStatus.APPROVED);
        
        Map<Long, Integer> citizenMultipliers = new HashMap<>(); // userId -> multiplier
        for (User citizen : provinceCitizensList) {
            citizenMultipliers.put(citizen.getId(), 1); // Default = 1x
        }
        
        for (DamageAssessment assessment : approvedAssessments) {
            User citizen = assessment.getCitizen();
            if (citizen != null && citizenMultipliers.containsKey(citizen.getId())) {
                int level = assessment.getDamageLevel() != null ? assessment.getDamageLevel() : 1;
                if (level > citizenMultipliers.get(citizen.getId())) {
                    citizenMultipliers.put(citizen.getId(), level); // Level 2 -> 2x, Level 3 -> 3x
                }
            }
        }
        
        // 3. Calculate Base Token
        long totalShares = 0;
        for (Integer multiplier : citizenMultipliers.values()) {
            totalShares += multiplier;
        }
        
        if (totalShares == 0) {
            throw new IllegalArgumentException("Tổng hệ số bằng 0, không thể phân chia.");
        }
        
        long baseAmount = remainingFunds / totalShares;
        if (baseAmount <= 0) {
            throw new IllegalArgumentException("Ngân sách còn lại (" + remainingFunds + ") quá ít để phân chia cho tổng hệ số (" + totalShares + ").");
        }
        
        // 4. Group by multiplier and Execute Delivery
        Map<Integer, List<User>> groupByMultiplier = new HashMap<>();
        groupByMultiplier.put(1, new ArrayList<>());
        groupByMultiplier.put(2, new ArrayList<>());
        groupByMultiplier.put(3, new ArrayList<>());
        
        for (User citizen : provinceCitizensList) {
            String wallet = citizen.getWalletAddress();
            if (wallet == null || wallet.isBlank()) continue;
            
            Integer m = citizenMultipliers.get(citizen.getId());
            groupByMultiplier.get(m).add(citizen);
        }
        
        boolean anySuccess = false;
        StringBuilder resultMessage = new StringBuilder("Phân bổ quỹ khu vực " + province + " thành công (Hệ số cơ bản: " + baseAmount + "): ");
        
        for (Map.Entry<Integer, List<User>> entry : groupByMultiplier.entrySet()) {
            int multiplier = entry.getKey();
            List<User> list = entry.getValue();
            if (list.isEmpty()) continue;
            
            long amountToDistribute = baseAmount * multiplier;
            List<String> walletAddresses = list.stream().map(User::getWalletAddress).collect(Collectors.toList());
            
            try {
                String txHash = blockchainService.airdrop(province, walletAddresses, BigInteger.valueOf(amountToDistribute));
                
                for (User citizen : list) {
                    transactionHistoryRepository.save(new TransactionHistory(
                        null,
                        citizen.getId(),
                        TransactionHistory.TxType.AIRDROP,
                        amountToDistribute,
                        "Nhận phân bổ quỹ khu vực " + province + " (Hệ số " + multiplier + "x)",
                        txHash
                    ));
                }
                
                anySuccess = true;
                resultMessage.append(list.size()).append(" người (").append(multiplier).append("x); ");
                
            } catch (Exception ex) {
                log.error("Batch airdrop failed for multiplier {}x in {}: {}", multiplier, province, ex.getMessage());
                // Fallback to individual
                for (User citizen : list) {
                    try {
                        String singleTx = blockchainService.mintToken(citizen.getWalletAddress(), TOKEN_ID, BigInteger.valueOf(amountToDistribute));
                        transactionHistoryRepository.save(new TransactionHistory(
                            null, citizen.getId(), TransactionHistory.TxType.AIRDROP, amountToDistribute,
                            "Nhận phân bổ quỹ khu vực " + province + " (Hệ số " + multiplier + "x) [Fallback]", singleTx
                        ));
                        anySuccess = true;
                    } catch (Exception individualEx) {
                        log.error("Failed mint fallback for {}: {}", citizen.getUsername(), individualEx.getMessage());
                    }
                }
            }
        }
        
        if (!anySuccess) {
            throw new RuntimeException("Tất cả giao dịch Blockchain đều thất bại, vui lòng kiểm tra lại");
        }
        
        // Cuối cùng cập nhật DB auto Airdrop
        pool.setIsAutoAirdrop(true);
        campaignPoolRepository.save(pool);

        return resultMessage.toString();
    }
}
