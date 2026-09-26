package com.back.catchmate.inquiry.application.dto.result;

import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record AdminInquiryResult(
        Long inquiryId,
        Long userId,
        String userNickname,
        String type,
        String content,
        String status,
        LocalDateTime createdAt) {

    // 작성자가 탈퇴해 조회되지 않으면 목록 전체를 실패시키지 않고 닉네임만 비운다.
    public static AdminInquiryResult of(Inquiry inquiry, UserInfo writer) {
        return new AdminInquiryResult(
                inquiry.getId(),
                inquiry.getUserId(),
                writer != null ? writer.nickName() : null,
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt());
    }
}
