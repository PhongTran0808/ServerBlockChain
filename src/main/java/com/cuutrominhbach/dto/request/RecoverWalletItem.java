package com.cuutrominhbach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecoverWalletItem(
    @NotNull Long batchId,
    @NotBlank String walletAddress,
    @NotBlank String role
) {}
