package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminEnrollInfo(
        Long enrollId,
        Long userId,
        String acceptStatus,
        LocalDateTime requestedAt
) {
}
