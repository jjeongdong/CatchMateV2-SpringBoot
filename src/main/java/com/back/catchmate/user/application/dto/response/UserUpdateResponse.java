package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.user.domain.model.User;

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
        ClubResponse club,
        LocalDate birthDate,
        String watchStyle
) {
    public static UserUpdateResponse from(User user, Club club) {
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
                club != null ? ClubResponse.from(club) : null,
                user.getBirthDate(),
                user.getWatchStyle()
        );
    }
}
