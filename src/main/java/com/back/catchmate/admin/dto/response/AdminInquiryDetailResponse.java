package com.back.catchmate.admin.dto.response;

import com.back.catchmate.inquiry.dto.response.InquirySummary;
import com.back.catchmate.user.dto.response.UserSummary;
import java.time.LocalDateTime;

public record AdminInquiryDetailResponse(
        Long inquiryId,
        Long userId,
        String userNickname,
        String userEmail,
        String userProfileImage,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt) {
    public static AdminInquiryDetailResponse from(InquirySummary inquiry, UserSummary user) {
        return new AdminInquiryDetailResponse(
                inquiry.inquiryId(),
                user.userId(),
                user.nickName(),
                user.email(),
                user.profileImageUrl(),
                inquiry.type(),
                inquiry.content(),
                inquiry.answer(),
                inquiry.status(),
                inquiry.createdAt());
    }
}
