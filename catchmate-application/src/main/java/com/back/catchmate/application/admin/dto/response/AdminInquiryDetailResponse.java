package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminInquiryDetailResponse {
    private Long inquiryId;
    
    // 작성자 정보
    private Long userId;
    private String userNickname;
    private String userEmail;
    private String userProfileImage;

    private String type;    // 문의 유형
    private String content;
    private String status;  // 처리 상태
    private LocalDateTime createdAt;

    public static AdminInquiryDetailResponse from(Inquiry inquiry) {
        return AdminInquiryDetailResponse.builder()
                .inquiryId(inquiry.getId())
                .userId(inquiry.getUser().getId())
                .userNickname(inquiry.getUser().getNickName())
                .userEmail(inquiry.getUser().getEmail())
                .userProfileImage(inquiry.getUser().getProfileImageUrl())
                .type(inquiry.getType().name())
                .content(inquiry.getContent())
                .status(inquiry.getStatus().name())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
