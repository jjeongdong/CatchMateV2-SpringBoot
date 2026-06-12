package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.Authority;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.club.application.dto.response.ClubResponse;
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
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getWatchStyle(),
                ClubResponse.from(user.getClub()),
                user.getAuthority()
        );
    }
}
