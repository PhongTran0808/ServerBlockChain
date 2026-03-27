package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.dto.request.CreateDistributionRoundRequest;
import com.cuutrominhbach.dto.response.ClaimableDistributionResponse;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DistributionService {


    private final CampaignPoolRepository campaignPoolRepository;
    private final UserRepository userRepository;
    private final DistributionRoundRepository distributionRoundRepository;
    private final DistributionClaimRepository distributionClaimRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final BlockchainService blockchainService;
    private final ObjectMapper objectMapper;
    private final GovernmentAdministrativeService governmentAdministrativeService;

    public DistributionService(CampaignPoolRepository campaignPoolRepository,
                               UserRepository userRepository,
                               DistributionRoundRepository distributionRoundRepository,
                               DistributionClaimRepository distributionClaimRepository,
                               TransactionHistoryRepository transactionHistoryRepository,
                               BlockchainService blockchainService,
                              ObjectMapper objectMapper,
                              GovernmentAdministrativeService governmentAdministrativeService) {
        this.campaignPoolRepository = campaignPoolRepository;
        this.userRepository = userRepository;
        this.distributionRoundRepository = distributionRoundRepository;
        this.distributionClaimRepository = distributionClaimRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.blockchainService = blockchainService;
        this.objectMapper = objectMapper;
        this.governmentAdministrativeService = governmentAdministrativeService;
    }

    @Transactional
    public DistributionRound createRound(CreateDistributionRoundRequest request) {
        if (request == null || request.province() == null || request.province().isBlank()) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("Số tiền phân phối phải lớn hơn 0");
        }

        String province = request.province().trim();
        if (!governmentAdministrativeService.isValidProvince(province)) {
            throw new IllegalArgumentException("Tỉnh/Thành không hợp lệ theo dữ liệu địa giới hành chính chính phủ");
        }
        
        CampaignPool campaign = campaignPoolRepository.findByProvince(province)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy campaign theo tỉnh"));

        boolean hasLegacyAirdrop = transactionHistoryRepository.findAll().stream()
            .anyMatch(tx -> tx.getType() == TransactionHistory.TxType.IN
                && tx.getNote() != null
                && tx.getNote().equals("Nhận cứu trợ campaign " + province));
        if (hasLegacyAirdrop) {
            throw new IllegalArgumentException("Tỉnh này đã có phân phối legacy airdrop, không thể tạo Merkle round để tránh overlap");
        }

        long totalFund = campaign.getTotalFund() == null ? 0L : campaign.getTotalFund();
        long allocated = distributionRoundRepository.findByProvinceOrderByCreatedAtDesc(province)
            .stream()
            .mapToLong(r -> r.getTotalAmount() == null ? 0L : r.getTotalAmount())
            .sum();
        long remaining = Math.max(0L, totalFund - allocated);
        if (request.amount() > remaining) {
            throw new IllegalArgumentException("Số tiền vượt quá ngân quỹ còn lại của campaign");
        }

        List<User> citizens = userRepository.findByRoleAndProvince(Role.CITIZEN, province)
                .stream()
                .filter(u -> u.getWalletAddress() != null && !u.getWalletAddress().isBlank())
                .collect(Collectors.toList());

        if (citizens.isEmpty()) {
            throw new IllegalArgumentException("Không có citizen hợp lệ để phân phối");
        }

        long share = request.amount() / citizens.size();
        if (share <= 0) {
            throw new IllegalArgumentException("Số tiền quá nhỏ để chia đều");
        }

        List<Allocation> allocations = new ArrayList<>();
        for (int i = 0; i < citizens.size(); i++) {
            User citizen = citizens.get(i);
            Allocation a = new Allocation();
            a.setIndex(i);
            a.setCitizenId(citizen.getId());
            a.setWalletAddress(citizen.getWalletAddress());
            a.setAmount(share);
            a.setLeafHash(leafHash(citizen.getId(), share));
            allocations.add(a);
        }

        List<String> leaves = allocations.stream().map(Allocation::getLeafHash).collect(Collectors.toList());
        String merkleRoot = buildRoot(leaves);

        DistributionRound round = new DistributionRound();
        round.setCampaignPoolId(campaign.getId());
        round.setProvince(province);
        round.setCampaignCode(campaign.getCampaignCode());
        round.setTotalAmount(share * citizens.size());
        round.setRecipientsCount(citizens.size());
        round.setShareAmount(share);
        round.setMerkleRoot(merkleRoot);
        round.setStatus(DistributionRoundStatus.OFFCHAIN_READY);
        round.setCreatedAt(LocalDateTime.now());

        try {
            round.setAllocationsJson(objectMapper.writeValueAsString(allocations));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể serialize allocations", e);
        }

        String txHash = blockchainService.storeMerkleRoot(merkleRoot);
        round.setMerkleTxHash(txHash);
        round.setStatus(DistributionRoundStatus.ONCHAIN_STORED);

        return distributionRoundRepository.save(round);
    }

    public List<DistributionRound> getRounds(String province) {
        if (province == null || province.isBlank()) {
            List<DistributionRound> rounds = distributionRoundRepository.findAll();
            rounds.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            return rounds;
        }
        return distributionRoundRepository.findByProvinceOrderByCreatedAtDesc(province.trim());
    }

    public List<ClaimableDistributionResponse> getClaimableRounds(Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException("Chỉ citizen mới có thể claim cứu trợ");
        }

        List<DistributionRound> rounds = distributionRoundRepository.findByProvinceOrderByCreatedAtDesc(citizen.getProvince());
        List<ClaimableDistributionResponse> out = new ArrayList<>();

        for (DistributionRound round : rounds) {
            List<Allocation> allocations = parseAllocations(round.getAllocationsJson());
            Optional<Allocation> mine = allocations.stream()
                    .filter(a -> Objects.equals(a.getCitizenId(), citizenId))
                    .findFirst();
            if (mine.isEmpty()) {
                continue;
            }

            boolean claimed = distributionClaimRepository.existsByRoundIdAndCitizenId(round.getId(), citizenId);
            List<String> proof = buildProof(allocations.stream().map(Allocation::getLeafHash).collect(Collectors.toList()), mine.get().getIndex());

            ClaimableDistributionResponse item = new ClaimableDistributionResponse();
            item.setRoundId(round.getId());
            item.setProvince(round.getProvince());
            item.setCampaignCode(round.getCampaignCode());
            item.setAmount(mine.get().getAmount());
            item.setMerkleRoot(round.getMerkleRoot());
            item.setProof(proof);
            item.setClaimed(claimed);
            item.setCreatedAt(round.getCreatedAt());
            out.add(item);
        }

        return out;
    }

    @Transactional
    public Map<String, Object> claim(Long citizenId, Long roundId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException("Chỉ citizen mới có thể claim cứu trợ");
        }

        DistributionRound round = distributionRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vòng phân phối"));

        if (distributionClaimRepository.existsByRoundIdAndCitizenId(roundId, citizenId)) {
            throw new IllegalArgumentException("Bạn đã claim vòng này rồi");
        }

        List<Allocation> allocations = parseAllocations(round.getAllocationsJson());
        Allocation mine = allocations.stream()
                .filter(a -> Objects.equals(a.getCitizenId(), citizenId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bạn không nằm trong danh sách nhận của round này"));

        List<String> leaves = allocations.stream().map(Allocation::getLeafHash).collect(Collectors.toList());
        List<String> proof = buildProof(leaves, mine.getIndex());
        boolean valid = verifyProof(mine.getLeafHash(), mine.getIndex(), proof, round.getMerkleRoot());
        if (!valid) {
            throw new IllegalStateException("Merkle proof không hợp lệ");
        }

        if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Citizen chưa có ví blockchain để nhận token");
        }

        String txHash = blockchainService.claimDistribution(
            citizen.getWalletAddress(),
            BigInteger.valueOf(mine.getAmount()),
            proof
        );

        DistributionClaim claim = new DistributionClaim();
        claim.setRoundId(roundId);
        claim.setCitizenId(citizenId);
        claim.setAmount(mine.getAmount());
        try {
            claim.setProofJson(objectMapper.writeValueAsString(proof));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể serialize proof", e);
        }
        claim.setClaimTxHash(txHash);
        claim.setCreatedAt(LocalDateTime.now());
        distributionClaimRepository.save(claim);

        transactionHistoryRepository.save(new TransactionHistory(
                null,
                citizenId,
                TransactionHistory.TxType.IN,
                mine.getAmount(),
                "Nhận cứu trợ claim round #" + roundId,
                txHash
        ));

        return Map.of(
                "message", "Claim thành công",
                "roundId", roundId,
                "amount", mine.getAmount(),
                "txHash", txHash
        );
    }

    private List<Allocation> parseAllocations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Allocation>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Không thể đọc allocation snapshot", e);
        }
    }

    private static String leafHash(Long citizenId, Long amount) {
        String raw = citizenId + ":" + amount;
        return Numeric.toHexString(Hash.sha3(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private static String buildRoot(List<String> leaves) {
        if (leaves.isEmpty()) {
            return Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64);
        }

        List<String> level = new ArrayList<>(leaves);
        while (level.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                String left = level.get(i);
                String right = (i + 1 < level.size()) ? level.get(i + 1) : left;
                next.add(combine(left, right));
            }
            level = next;
        }
        return level.get(0);
    }

    private static List<String> buildProof(List<String> leaves, int index) {
        List<String> proof = new ArrayList<>();
        List<String> level = new ArrayList<>(leaves);
        int idx = index;

        while (level.size() > 1) {
            int siblingIndex = (idx % 2 == 0) ? idx + 1 : idx - 1;
            if (siblingIndex >= level.size()) {
                siblingIndex = idx;
            }
            proof.add(level.get(siblingIndex));

            List<String> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                String left = level.get(i);
                String right = (i + 1 < level.size()) ? level.get(i + 1) : left;
                next.add(combine(left, right));
            }

            idx = idx / 2;
            level = next;
        }

        return proof;
    }

    private static boolean verifyProof(String leaf, int index, List<String> proof, String expectedRoot) {
        String hash = leaf;
        int idx = index;
        for (String sibling : proof) {
            if (idx % 2 == 0) {
                hash = combine(hash, sibling);
            } else {
                hash = combine(sibling, hash);
            }
            idx = idx / 2;
        }
        return hash != null && hash.equalsIgnoreCase(expectedRoot);
    }

    private static String combine(String leftHex, String rightHex) {
        byte[] left = Numeric.hexStringToByteArray(leftHex);
        byte[] right = Numeric.hexStringToByteArray(rightHex);
        byte[] merged = new byte[left.length + right.length];
        System.arraycopy(left, 0, merged, 0, left.length);
        System.arraycopy(right, 0, merged, left.length, right.length);
        return Numeric.toHexString(Hash.sha3(merged));
    }

    public static class Allocation {
        private int index;
        private Long citizenId;
        private String walletAddress;
        private Long amount;
        private String leafHash;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public Long getCitizenId() { return citizenId; }
        public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }
        public String getWalletAddress() { return walletAddress; }
        public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getLeafHash() { return leafHash; }
        public void setLeafHash(String leafHash) { this.leafHash = leafHash; }
    }
}
