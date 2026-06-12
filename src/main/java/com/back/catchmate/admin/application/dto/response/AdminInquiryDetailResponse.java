package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.user.domain.model.User;

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
    public static AdminInquiryDetailResponse from(Inquiry inquiry, User user) {
        return new AdminInquiryDetailResponse(
                inquiry.getId(),
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
