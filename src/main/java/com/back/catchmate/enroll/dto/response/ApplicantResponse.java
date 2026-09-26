package com.back.catchmate.enroll.dto.response;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;

public record ApplicantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String favoriteClub,
        String watchStyle) {
    public static ApplicantResponse from(UserInfo user, ClubInfo club) {
        return new ApplicantResponse(
                user.userId(),
                user.nickName(),
                user.profileImageUrl(),
                String.valueOf(user.gender()),
                String.valueOf(user.birthDate()),
                club != null ? club.name() : null,
                user.watchStyle());
    }
}
