package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDateTime;

public record EnrollInternalResponse(
        Long enrollId,
        Long userId,
        Long boardId,
        String description,
        String acceptStatus,
        boolean newEnroll,
        LocalDateTime requestedAt
) {
}
