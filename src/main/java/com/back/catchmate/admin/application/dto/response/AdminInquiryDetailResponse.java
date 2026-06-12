package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.inquiry.domain.model.Inquiry;

import java.time.LocalDateTime;

public record AdminInquiryDetailResponse(
        Long inquiryId,
        Long userId,
        String userNickname,
        String userEmail,
        String userProfileImage,
        String type,
        String content,
        String status,
        LocalDateTime createdAt
) {
    public static AdminInquiryDetailResponse from(Inquiry inquiry) {
        return new AdminInquiryDetailResponse(
                inquiry.getId(),
                inquiry.getUser().getId(),
                inquiry.getUser().getNickName(),
                inquiry.getUser().getEmail(),
                inquiry.getUser().getProfileImageUrl(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
