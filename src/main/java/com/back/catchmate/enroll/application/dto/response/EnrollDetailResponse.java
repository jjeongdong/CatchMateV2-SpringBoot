package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record EnrollDetailResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        UserResponse applicant,
        BoardResponse boardResponse
) {
    public static EnrollDetailResponse from(Enroll enroll, User applicant, BoardResponse boardResponse) {
        return new EnrollDetailResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                UserResponse.from(applicant),
                boardResponse
        );
    }
}
