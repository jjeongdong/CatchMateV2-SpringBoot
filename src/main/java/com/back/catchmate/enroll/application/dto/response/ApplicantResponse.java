package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;

public record ApplicantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String favoriteClub,
        String watchStyle
) {
    public static ApplicantResponse from(EnrollUserInfo user, EnrollClubInfo club) {
        return new ApplicantResponse(
                user.userId(),
                user.nickName(),
                user.profileImageUrl(),
                String.valueOf(user.gender()),
                String.valueOf(user.birthDate()),
                club != null ? club.name() : null,
                user.watchStyle()
        );
    }
}
