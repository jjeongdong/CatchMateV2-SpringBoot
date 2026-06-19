package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

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
    public static AdminInquiryResponse from(AdminInquiryInfo inquiry, AdminUserInfo user) {
        return new AdminInquiryResponse(
                inquiry.inquiryId(),
                user.userId(),
                user.nickName(),
                inquiry.type(),
                inquiry.content(),
                inquiry.status(),
                inquiry.createdAt()
        );
    }
}
