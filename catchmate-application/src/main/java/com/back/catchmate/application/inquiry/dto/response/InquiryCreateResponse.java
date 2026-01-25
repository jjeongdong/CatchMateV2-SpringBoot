package com.back.catchmate.application.inquiry.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
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
