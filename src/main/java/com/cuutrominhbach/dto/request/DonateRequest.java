package com.cuutrominhbach.dto.request;

public record DonateRequest(Long amount, String province, String password, String pin) {
    // Hỗ trợ cả 2 field: 'pin' (từ CitizenHome) và 'password' (legacy)
    public String resolvedPin() {
        return pin != null ? pin : password;
    }
}
