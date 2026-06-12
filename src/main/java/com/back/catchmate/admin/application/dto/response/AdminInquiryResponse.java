package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.inquiry.domain.model.Inquiry;

import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long inquiryId,
        Long userId,
        String userNickname,
        String type,
        String content,
        String status,
        LocalDateTime createdAt
) {
    public static AdminInquiryResponse from(Inquiry inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getUser().getId(),
                inquiry.getUser().getNickName(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
