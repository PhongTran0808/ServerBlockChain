package com.cuutrominhbach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WalletUpdateRequest(
    @NotBlank(message = "Địa chỉ ví không được để trống")
    @Pattern(
        regexp = "^0x[a-fA-F0-9]{40}$",
        message = "Địa chỉ ví không hợp lệ. Định dạng yêu cầu: 0x theo sau bởi 40 ký tự hex (0-9, a-f, A-F)."
    )
    String walletAddress
) {}
