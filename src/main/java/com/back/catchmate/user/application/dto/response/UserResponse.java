package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.Authority;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String nickName;
    private String email;
    private String profileImageUrl;

    private char gender;
    private LocalDate birthDate;
    private String watchStyle;

    private ClubResponse club;
    private Authority authority;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .nickName(user.getNickName())
                .club(ClubResponse.from(user.getClub()))
                .birthDate(user.getBirthDate())
                .watchStyle(user.getWatchStyle())
                .authority(user.getAuthority())
                .build();
    }
}
