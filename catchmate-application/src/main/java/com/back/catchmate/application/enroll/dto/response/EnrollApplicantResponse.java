package com.back.catchmate.application.enroll.dto.response;

import com.back.catchmate.domain.enroll.model.Enroll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollApplicantResponse {
    private Long enrollId;
    private String description;
    private LocalDateTime requestDate;
    private boolean newEnroll;
    private ApplicantResponse applicantResponse;

    public static EnrollApplicantResponse from(Enroll enroll) {
        return EnrollApplicantResponse.builder()
                .enrollId(enroll.getId())
                .description(enroll.getDescription())
                .requestDate(enroll.getRequestedAt())
                .newEnroll(true)
                .applicantResponse(ApplicantResponse.from(enroll.getUser()))
                .build();
    }
}
