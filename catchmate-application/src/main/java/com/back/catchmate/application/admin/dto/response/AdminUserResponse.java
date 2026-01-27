package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {
    private Long userId;
    private String nickName;
    private String email;
    private String clubName;
    private String authority;
    private boolean isReported;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .userId(user.getId())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .clubName(user.getClub().getName())
                .authority(user.getAuthority().name())
                .isReported(user.isReported())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
