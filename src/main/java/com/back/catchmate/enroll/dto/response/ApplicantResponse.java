package com.back.catchmate.enroll.dto.response;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.user.dto.response.UserSummary;

public record ApplicantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String favoriteClub,
        String watchStyle
) {
    public static ApplicantResponse from(UserSummary user, ClubSummary club) {
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
