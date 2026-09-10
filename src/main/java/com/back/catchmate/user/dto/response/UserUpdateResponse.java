package com.back.catchmate.user.dto.response;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.user.entity.User;

import java.time.LocalDate;

public record UserUpdateResponse(
        Long userId,
        String email,
        String profileImageUrl,
        char gender,
        char allAlarm,
        char chatAlarm,
        char enrollAlarm,
        char eventAlarm,
        String nickName,
        ClubSummary club,
        LocalDate birthDate,
        String watchStyle
) {
    public static UserUpdateResponse from(User user, ClubSummary club) {
        return new UserUpdateResponse(
                user.getId(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getAllAlarm(),
                user.getChatAlarm(),
                user.getEnrollAlarm(),
                user.getEventAlarm(),
                user.getNickName(),
                club,
                user.getBirthDate(),
                user.getWatchStyle()
        );
    }
}
