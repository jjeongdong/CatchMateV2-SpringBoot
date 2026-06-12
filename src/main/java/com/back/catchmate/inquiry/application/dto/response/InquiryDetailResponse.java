package com.back.catchmate.inquiry.application.dto.response;

import com.back.catchmate.inquiry.domain.model.Inquiry;
import java.time.LocalDateTime;

public record InquiryDetailResponse(
        Long inquiryId,
        String nickname,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt
) {
    public static InquiryDetailResponse from(Inquiry inquiry, String nickname) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                nickname,
                inquiry.getType().getDescription(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().getDescription(),
                inquiry.getCreatedAt()
        );
    }
}
