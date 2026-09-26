package com.back.catchmate.inquiry.application.dto.result;

import com.back.catchmate.inquiry.domain.Inquiry;
import java.time.LocalDateTime;

// 사용자 화면용이라 유형·상태를 한국어 설명으로 내린다.
public record InquiryResult(
        Long inquiryId,
        String nickname,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt) {

    public static InquiryResult of(Inquiry inquiry, String nickname) {
        return new InquiryResult(
                inquiry.getId(),
                nickname,
                inquiry.getType().description(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().description(),
                inquiry.getCreatedAt());
    }
}
