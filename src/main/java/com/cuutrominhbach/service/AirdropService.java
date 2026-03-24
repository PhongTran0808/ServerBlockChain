package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
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

    public AirdropService(UserRepository userRepository, BlockchainService blockchainService) {
        this.userRepository = userRepository;
        this.blockchainService = blockchainService;
    }

    /**
     * Airdrop tokens to all CITIZEN users in a province.
     * Returns list of transaction hashes.
     */
    public List<String> airdrop(String province, Long amountPerCitizen) {
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
            } catch (Exception e) {
                log.error("Airdrop thất bại cho citizen {}: {}", citizen.getId(), e.getMessage());
            }
        }
        return txHashes;
    }
}
