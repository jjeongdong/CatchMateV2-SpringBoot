package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminInquiryResponse {
    private Long inquiryId;
    private Long userId;
    private String userNickname;
    private String type;
    private String content;
    private String status; // 답변 대기 / 답변 완료
    private LocalDateTime createdAt;

    public static AdminInquiryResponse from(Inquiry inquiry) {
        return AdminInquiryResponse.builder()
                .inquiryId(inquiry.getId())
                .userId(inquiry.getUser().getId())
                .userNickname(inquiry.getUser().getNickName())
                .type(inquiry.getType().name())
                .content(inquiry.getContent())
                .status(inquiry.getStatus().name())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
