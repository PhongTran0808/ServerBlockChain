package com.cuutrominhbach.dto.response;

public record RegisterResponse(
        Long userId,
        String username,
        String role,
        String approvalStatus
) {}
