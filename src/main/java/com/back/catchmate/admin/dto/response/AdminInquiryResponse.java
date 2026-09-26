package com.back.catchmate.admin.dto.response;

import com.back.catchmate.inquiry.dto.response.InquirySummary;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long inquiryId,
        Long userId,
        String userNickname,
        String type,
        String content,
        String status,
        LocalDateTime createdAt) {
    public static AdminInquiryResponse from(InquirySummary inquiry, UserInfo user) {
        return new AdminInquiryResponse(
                inquiry.inquiryId(),
                user.userId(),
                user.nickName(),
                inquiry.type(),
                inquiry.content(),
                inquiry.status(),
                inquiry.createdAt());
    }
}
