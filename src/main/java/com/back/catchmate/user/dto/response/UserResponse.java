package com.back.catchmate.user.dto.response;

import com.back.catchmate.club.application.dto.api.ClubInfo;
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
        ClubInfo club,
        String authority) {
    public static UserResponse from(User user, ClubInfo club) {
        return new UserResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getWatchStyle(),
                club,
                user.getAuthority() != null ? user.getAuthority().name() : null);
    }
}
