package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.request.LoginRequest;
import com.cuutrominhbach.dto.request.RegisterRequest;
import com.cuutrominhbach.dto.response.LoginResponse;
import com.cuutrominhbach.dto.response.RegisterResponse;
import com.cuutrominhbach.entity.CampaignPool;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    // Mã chiến dịch hợp lệ hiện tại — do tỉnh phát hành trong đợt tuyên truyền
    private static final String VALID_CAMPAIGN_CODE = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CampaignPoolRepository campaignPoolRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       CampaignPoolRepository campaignPoolRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.campaignPoolRepository = campaignPoolRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getHashPassword())) {
            throw new AuthException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if ((user.getRole() == Role.SHOP || user.getRole() == Role.TRANSPORTER)
                && !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new AuthException("Tài khoản đối tác chưa được phê duyệt");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getWalletAddress(),
                user.getId()
        );
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new AuthException("Dữ liệu đăng ký không hợp lệ");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new AuthException("Tên đăng nhập không được để trống");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new AuthException("Mật khẩu không được để trống");
        }
        if (request.role() == null || request.role().isBlank()) {
            throw new AuthException("Vai trò không được để trống");
        }

        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new AuthException("Tên đăng nhập đã tồn tại");
        }

        Role role;
        try {
            role = Role.valueOf(request.role().trim().toUpperCase());
        } catch (Exception ex) {
            throw new AuthException("Vai trò không hợp lệ");
        }

        // ── Validate mã chiến dịch cho CITIZEN ───────────────────────────────
        if (role == Role.CITIZEN) {
            String code = request.campaignCode();
            if (code == null || code.isBlank()) {
                throw new AuthException("Vui lòng nhập mã chiến dịch cứu trợ");
            }
            if (!VALID_CAMPAIGN_CODE.equals(code.trim())) {
                throw new AuthException("Mã chiến dịch không hợp lệ. Vui lòng liên hệ chính quyền địa phương.");
            }
            if (request.province() == null || request.province().isBlank()) {
                throw new AuthException("Người dân phải chọn Tỉnh/Thành phố");
            }
        }

        Boolean isApproved = (role == Role.SHOP || role == Role.TRANSPORTER) ? null : true;

        // CITIZEN không lưu walletAddress từ request (ví tạo riêng bởi admin/seeder)
        // SHOP/TRANSPORTER cũng không lưu walletAddress từ đây (admin set sau)
        User user = User.builder()
                .username(username)
                .fullName(request.fullName() == null ? username : request.fullName().trim())
                .role(role)
                .walletAddress(null) // wallet được set riêng, không lấy từ form đăng ký
                .hashPassword(passwordEncoder.encode(request.password()))
                .province(request.province() == null ? null : request.province().trim())
                .isApproved(isApproved)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);

        // ── Tự động tạo CampaignPool cho tỉnh mới (nếu chưa có) ─────────────
        if (role == Role.CITIZEN && saved.getProvince() != null && !saved.getProvince().isBlank()) {
            String province = saved.getProvince();
            if (campaignPoolRepository.findByProvince(province).isEmpty()) {
                CampaignPool pool = CampaignPool.builder()
                        .campaignCode("CP-" + province.toUpperCase().replace(" ", "-"))
                        .province(province)
                        .totalFund(0L)
                        .isReceivingActive(true)
                        .isAutoAirdrop(false)
                        .updatedAt(LocalDateTime.now())
                        .build();
                campaignPoolRepository.save(pool);
            }
        }

        String approvalStatus = saved.getIsApproved() == null
                ? "PENDING"
                : (Boolean.TRUE.equals(saved.getIsApproved()) ? "APPROVED" : "REJECTED");

        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole().name(),
                approvalStatus
        );
    }
}
