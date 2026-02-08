package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.enroll.model.Enroll;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminEnrollmentResponse {
    private Long enrollId;
    private Long userId;
    private String profileImageUrl;
    private String nickName;
    private String clubName;
    private Character gender;
    private String email;
    private String provider;
    private String status;
    private LocalDateTime requestedAt;

    public static AdminEnrollmentResponse from(Enroll enroll) {
        return AdminEnrollmentResponse.builder()
                .enrollId(enroll.getId())
                .userId(enroll.getUser().getId())
                .profileImageUrl(enroll.getUser().getProfileImageUrl())
                .nickName(enroll.getUser().getNickName())
                .clubName(enroll.getUser().getClub().getName())
                .gender(enroll.getUser().getGender())
                .email(enroll.getUser().getEmail())
                .provider(enroll.getUser().getProvider().name())
                .status(enroll.getAcceptStatus().name())
                .requestedAt(enroll.getRequestedAt())
                .build();
    }
}
