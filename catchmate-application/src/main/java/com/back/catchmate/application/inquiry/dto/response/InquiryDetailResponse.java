package com.back.catchmate.application.inquiry.dto.response;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InquiryDetailResponse {
    private Long inquiryId;
    private String nickname;
    private String type;
    private String content;
    private String answer;
    private String status;
    private LocalDateTime createdAt;

    public static InquiryDetailResponse from(Inquiry inquiry) {
        return InquiryDetailResponse.builder()
                .inquiryId(inquiry.getId())
                .nickname(inquiry.getUser().getNickName())
                .type(inquiry.getType().getDescription())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus().getDescription())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
