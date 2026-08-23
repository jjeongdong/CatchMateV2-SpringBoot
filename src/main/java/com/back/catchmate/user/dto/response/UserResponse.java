package com.back.catchmate.user.dto.response;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.user.entity.User;

import java.time.LocalDate;

public record UserResponse(
        Long userId,
        String nickName,
        String email,
        String profileImageUrl,
        char gender,
        LocalDate birthDate,
        String watchStyle,
        ClubSummary club,
        String authority
) {
    public static UserResponse from(User user, ClubSummary club) {
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
