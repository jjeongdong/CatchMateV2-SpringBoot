package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.user.domain.model.User;

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
    public static AdminInquiryResponse from(Inquiry inquiry, User user) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                user.getId(),
                user.getNickName(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
