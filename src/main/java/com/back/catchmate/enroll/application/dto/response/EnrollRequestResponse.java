package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import java.time.LocalDateTime;

public record EnrollRequestResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        BoardResponse boardResponse
) {
    public static EnrollRequestResponse from(Enroll enroll, boolean bookMarked) {
        return new EnrollRequestResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                BoardResponse.from(enroll.getBoard(), bookMarked)
        );
    }
}
