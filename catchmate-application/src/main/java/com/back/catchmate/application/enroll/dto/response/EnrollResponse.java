package com.back.catchmate.application.enroll.dto.response;

import com.back.catchmate.domain.enroll.model.Enroll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollResponse {
    private Long enrollId;
    private String description;
    private boolean newEnroll;
    private LocalDateTime requestDate;
    private ApplicantResponse applicant;

    public static EnrollResponse from(Enroll enroll) {
        return EnrollResponse.builder()
                .enrollId(enroll.getId())
                .description(enroll.getDescription())
                .requestDate(enroll.getRequestedAt())
                .newEnroll(enroll.isNew())
                .applicant(ApplicantResponse.from(enroll.getUser()))
                .build();
    }
}
