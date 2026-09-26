package com.back.catchmate.inquiry.application.dto.result;

import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record AdminInquiryDetailResult(
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

    public static AdminInquiryDetailResult of(Inquiry inquiry, UserInfo writer) {
        return new AdminInquiryDetailResult(
                inquiry.getId(),
                inquiry.getUserId(),
                writer.nickName(),
                writer.email(),
                writer.profileImageUrl(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt());
    }
}
