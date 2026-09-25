package com.back.catchmate.enroll.dto.response;

import java.time.LocalDateTime;

public record EnrollSummary(
        Long enrollId,
        Long userId,
        Long boardId,
        String description,
        String acceptStatus,
        boolean newEnroll,
        LocalDateTime requestedAt) {}
