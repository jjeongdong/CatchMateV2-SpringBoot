package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record EnrollResponse(
        Long enrollId,
        String description,
        boolean newEnroll,
        LocalDateTime requestDate,
        ApplicantResponse applicant
) {
    public static EnrollResponse from(Enroll enroll, User user, Club club) {
        return new EnrollResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.isNewEnroll(),
                enroll.getRequestedAt(),
                ApplicantResponse.from(user, club)
        );
    }
}
