package com.cuutrominhbach.dto.response;

public record LoginResponse(String token, String role, String walletAddress, Long userId) {}
