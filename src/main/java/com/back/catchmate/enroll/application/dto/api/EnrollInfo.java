package com.back.catchmate.enroll.application.dto.api;

import com.back.catchmate.enroll.domain.Enroll;
import java.time.LocalDateTime;

// 타 BC 계약. acceptStatus 는 AcceptStatus 이름 (PENDING, ACCEPTED, REJECTED).
public record EnrollInfo(
        Long enrollId,
        Long userId,
        Long boardId,
        String description,
        String acceptStatus,
        boolean newEnroll,
        LocalDateTime requestedAt) {
    public static EnrollInfo from(Enroll enroll) {
        return new EnrollInfo(
                enroll.getId(),
                enroll.getUserId(),
                enroll.getBoardId(),
                enroll.getDescription(),
                enroll.getAcceptStatus().name(),
                enroll.isNewEnroll(),
                enroll.getRequestedAt());
    }
}
