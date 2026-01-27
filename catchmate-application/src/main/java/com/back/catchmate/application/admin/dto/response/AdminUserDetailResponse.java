package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserDetailResponse {
    private Long userId;
    private String email;
    private String nickName;
    private String provider;
    private Character gender;
    private LocalDate birthDate;
    private String clubName;
    private String watchStyle;
    private String role;
    private boolean isReported;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserDetailResponse from(User user) {
        return AdminUserDetailResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .provider(user.getProvider().name())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .clubName(user.getClub().getName())
                .watchStyle(user.getWatchStyle())
                .role(user.getAuthority().name())
                .isReported(user.isReported())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
