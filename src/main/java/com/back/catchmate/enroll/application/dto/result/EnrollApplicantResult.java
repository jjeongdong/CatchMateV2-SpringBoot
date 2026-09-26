package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record EnrollApplicantResult(
        Long enrollId,
        String description,
        LocalDateTime requestDate,
        boolean newEnroll,
        ApplicantView applicantResponse) {
    public static EnrollApplicantResult of(Enroll enroll, UserInfo user, ClubInfo club) {
        return new EnrollApplicantResult(
                enroll.getId(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                enroll.isNewEnroll(),
                ApplicantView.of(user, club));
    }

    // 옛 ApplicantResponse 와 같은 JSON. 탈퇴한 신청자는 null.
    public record ApplicantView(
            Long userId,
            String nickname,
            String profileImageUrl,
            String gender,
            String ageRange,
            String favoriteClub,
            String watchStyle) {
        public static ApplicantView of(UserInfo user, ClubInfo club) {
            if (user == null) {
                return null;
            }
            return new ApplicantView(
                    user.userId(),
                    user.nickName(),
                    user.profileImageUrl(),
                    String.valueOf(user.gender()),
                    String.valueOf(user.birthDate()),
                    club != null ? club.name() : null,
                    user.watchStyle());
        }
    }
}
