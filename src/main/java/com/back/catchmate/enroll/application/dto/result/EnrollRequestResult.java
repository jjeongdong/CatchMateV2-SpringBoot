package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import java.time.LocalDateTime;

public record EnrollRequestResult(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        EnrollBoardResult boardResponse) {
    public static EnrollRequestResult of(Enroll enroll, EnrollBoardResult board) {
        return new EnrollRequestResult(
                enroll.getId(), enroll.getAcceptStatus(), enroll.getDescription(), enroll.getRequestedAt(), board);
    }
}
