package com.cuutrominhbach.dto.request;

public record RegisterRequest(
        String username,
        String password,
        String fullName,
        String role,
        String province,
        String walletAddress,
        // Mã chiến dịch do tỉnh cấp — dùng để xác thực citizen khi đăng ký
        String campaignCode
) {}
