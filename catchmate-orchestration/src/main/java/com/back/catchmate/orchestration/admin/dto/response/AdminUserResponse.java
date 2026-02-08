package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {
    private Long userId;
    private String profileImageUrl;
    private String nickName;
    private String email;
    private String clubName;
    private String gender;
    private String authority;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .userId(user.getId())
                .profileImageUrl(user.getProfileImageUrl())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .clubName(user.getClub().getName())
                .gender(user.getGender().toString())
                .authority(user.getAuthority().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
