package com.back.catchmate.orchestration.user.dto.response;

import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.club.dto.response.ClubResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
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
