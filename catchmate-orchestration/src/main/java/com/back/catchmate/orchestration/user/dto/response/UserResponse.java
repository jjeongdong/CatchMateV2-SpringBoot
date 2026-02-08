package com.back.catchmate.orchestration.user.dto.response;

import com.back.catchmate.domain.user.model.User;
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

    private Object club; // keep generic to avoid depending on application API club DTO

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .nickName(user.getNickName())
                .club(null)
                .birthDate(user.getBirthDate())
                .watchStyle(user.getWatchStyle())
                .build();
    }
}
