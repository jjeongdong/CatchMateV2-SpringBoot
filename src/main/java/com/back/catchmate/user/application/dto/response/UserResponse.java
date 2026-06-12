package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.user.domain.model.Authority;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDate;

public record UserResponse(
        Long userId,
        String nickName,
        String email,
        String profileImageUrl,
        char gender,
        LocalDate birthDate,
        String watchStyle,
        ClubResponse club,
        Authority authority
) {
    public static UserResponse from(User user, Club club) {
        return new UserResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getWatchStyle(),
                club != null ? ClubResponse.from(club) : null,
                user.getAuthority()
        );
    }
}
