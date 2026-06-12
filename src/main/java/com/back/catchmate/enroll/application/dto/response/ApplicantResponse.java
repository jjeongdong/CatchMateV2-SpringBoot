package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.user.domain.model.User;

public record ApplicantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String favoriteClub,
        String watchStyle
) {
    public static ApplicantResponse from(User user, Club club) {
        return new ApplicantResponse(
                user.getId(),
                user.getNickName(),
                user.getProfileImageUrl(),
                String.valueOf(user.getGender()),
                String.valueOf(user.getBirthDate()),
                club != null ? club.getName() : null,
                user.getWatchStyle()
        );
    }
}
