package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.dto.result.EnrollApplicantResult.ApplicantView;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;
import java.util.List;

public record EnrollReceivedResult(EnrollBoardResult boardResponse, List<EnrollView> enrollResponses) {

    public record EnrollView(
            Long enrollId, String description, boolean newEnroll, LocalDateTime requestDate, ApplicantView applicant) {
        public static EnrollView of(Enroll enroll, UserInfo user, ClubInfo club) {
            return new EnrollView(
                    enroll.getId(),
                    enroll.getDescription(),
                    enroll.isNewEnroll(),
                    enroll.getRequestedAt(),
                    ApplicantView.of(user, club));
        }
    }
}
