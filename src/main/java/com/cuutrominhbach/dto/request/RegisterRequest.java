package com.cuutrominhbach.dto.request;

public record RegisterRequest(
        String username,
        String password,
        String fullName,
        String role,
        String province,
        String walletAddress
) {}
