package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.application.port.out.dto.UserClubInfo;
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
        UserClubInfo club,
        String authority
) {
    public static UserResponse from(User user, UserClubInfo club) {
        return new UserResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getWatchStyle(),
                club,
                user.getAuthority() != null ? user.getAuthority().name() : null
        );
    }
}
