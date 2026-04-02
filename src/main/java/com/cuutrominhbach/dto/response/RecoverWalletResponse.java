package com.cuutrominhbach.dto.response;

import java.util.List;

public record RecoverWalletResponse(
        int updatedCount,
        int skippedCount,
        List<String> skippedReasons
) {}
