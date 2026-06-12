package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import java.time.LocalDateTime;

public record EnrollDetailResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        UserResponse applicant,
        BoardResponse boardResponse
) {
    public static EnrollDetailResponse from(Enroll enroll) {
        return new EnrollDetailResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                UserResponse.from(enroll.getUser()),
                BoardResponse.from(enroll.getBoard(), false)
        );
    }
}
