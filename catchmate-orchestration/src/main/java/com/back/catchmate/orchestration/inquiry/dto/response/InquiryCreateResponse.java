package com.back.catchmate.orchestration.inquiry.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InquiryCreateResponse {
    private Long inquiryId;
    private LocalDateTime createdAt;

    public static InquiryCreateResponse of(Long inquiryId) {
        return InquiryCreateResponse.builder()
                .inquiryId(inquiryId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
