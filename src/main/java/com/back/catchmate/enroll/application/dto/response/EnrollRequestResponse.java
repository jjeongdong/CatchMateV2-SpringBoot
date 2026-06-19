package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;

import java.time.LocalDateTime;

public record EnrollRequestResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        EnrollBoardSummary boardResponse
) {
    public static EnrollRequestResponse from(Enroll enroll, EnrollBoardSummary boardResponse) {
        return new EnrollRequestResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                boardResponse
        );
    }
}
