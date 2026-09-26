package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.enroll.application.dto.result.EnrollBoardResult.WriterView;
import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import java.time.LocalDateTime;

public record EnrollDetailResult(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        WriterView applicant,
        EnrollBoardResult boardResponse) {
    public static EnrollDetailResult of(Enroll enroll, WriterView applicant, EnrollBoardResult board) {
        return new EnrollDetailResult(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                applicant,
                board);
    }
}
