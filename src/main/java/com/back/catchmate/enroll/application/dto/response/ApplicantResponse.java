package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.user.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApplicantResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String gender;
    private String ageRange;
    private String favoriteClub;
    private String watchStyle;

    public static ApplicantResponse from(User user) {
        return ApplicantResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickName())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(String.valueOf(user.getGender()))
                .ageRange(String.valueOf(user.getBirthDate()))
                .favoriteClub(user.getClub() != null ? user.getClub().getName() : null)
                .watchStyle(user.getWatchStyle())
                .build();
    }
}
