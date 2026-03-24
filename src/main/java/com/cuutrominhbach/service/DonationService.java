package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.request.DonateRequest;
import com.cuutrominhbach.dto.response.DonateResponse;
import com.cuutrominhbach.entity.CampaignPool;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DonationService {

    private final UserRepository userRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final PasswordEncoder passwordEncoder;

    public DonationService(UserRepository userRepository,
                           CampaignPoolRepository campaignPoolRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public DonateResponse donate(Long userId, DonateRequest request) {
        // 1. Validate amount
        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        // 2. Query user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại"));

        // 3. Verify password
        if (!passwordEncoder.matches(request.password(), user.getHashPassword())) {
            throw new AuthException("Mã PIN không đúng");
        }

        // 4. Find or create CampaignPool
        Optional<CampaignPool> existing = campaignPoolRepository.findByProvince(request.province());
        CampaignPool pool;
        if (existing.isPresent()) {
            pool = existing.get();
            pool.setTotalFund(pool.getTotalFund() + request.amount());
            pool.setUpdatedAt(LocalDateTime.now());
        } else {
            pool = CampaignPool.builder()
                    .province(request.province())
                    .totalFund(request.amount())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        // 5. Save and return
        CampaignPool saved = campaignPoolRepository.save(pool);
        return new DonateResponse("Quyên góp thành công", saved.getTotalFund(), saved.getProvince());
    }
}
