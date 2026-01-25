package com.back.catchmate.application.inquiry.dto.response;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryDetailResponse {
    private Long inquiryId;
    private String type;
    private String title;
    private String content;
    private String answer;
    private String status;
    private LocalDateTime createdAt;

    public static InquiryDetailResponse from(Inquiry inquiry) {
        return InquiryDetailResponse.builder()
                .inquiryId(inquiry.getId())
                .type(inquiry.getType().getDescription())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus().getDescription())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
