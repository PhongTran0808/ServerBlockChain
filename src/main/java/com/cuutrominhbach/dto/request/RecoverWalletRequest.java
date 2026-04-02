package com.cuutrominhbach.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecoverWalletRequest(
    @NotNull
    @Size(min = 1)
    List<RecoverWalletItem> items
) {}
