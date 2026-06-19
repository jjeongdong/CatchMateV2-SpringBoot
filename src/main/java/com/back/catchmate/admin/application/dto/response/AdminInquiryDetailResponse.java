package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

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
    public static AdminInquiryDetailResponse from(AdminInquiryInfo inquiry, AdminUserInfo user) {
        return new AdminInquiryDetailResponse(
                inquiry.inquiryId(),
                user.userId(),
                user.nickName(),
                user.email(),
                user.profileImageUrl(),
                inquiry.type(),
                inquiry.content(),
                inquiry.status(),
                inquiry.createdAt()
        );
    }
}
