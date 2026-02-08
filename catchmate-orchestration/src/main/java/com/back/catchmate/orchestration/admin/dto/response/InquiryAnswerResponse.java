package com.back.catchmate.orchestration.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryAnswerResponse {
    private Long inquiryId;
    private Long userId;
    private LocalDateTime answeredAt;

    public static InquiryAnswerResponse of(Long inquiryId, Long userId) {
        return InquiryAnswerResponse.builder()
                .inquiryId(inquiryId)
                .userId(userId)
                .answeredAt(LocalDateTime.now())
                .build();
    }
}
