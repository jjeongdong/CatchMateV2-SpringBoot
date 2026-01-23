package com.back.catchmate.application.user.dto.response;

import com.back.catchmate.application.club.dto.response.ClubResponse;
import com.back.catchmate.domain.user.model.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserUpdateResponse {
    private Long userId;
    private String email;
    private String profileImageUrl;
    private char gender;
    private char allAlarm;
    private char chatAlarm;
    private char enrollAlarm;
    private char eventAlarm;
    private String nickName;
    private ClubResponse club;
    private LocalDate birthDate;
    private String watchStyle;

    public static UserUpdateResponse from(User user) {
        return UserUpdateResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .allAlarm(user.getAllAlarm())
                .chatAlarm(user.getChatAlarm())
                .enrollAlarm(user.getEnrollAlarm())
                .eventAlarm(user.getEventAlarm())
                .nickName(user.getNickName())
                .club(ClubResponse.from(user.getClub()))
                .birthDate(user.getBirthDate())
                .watchStyle(user.getWatchStyle())
                .build();
    }
}
