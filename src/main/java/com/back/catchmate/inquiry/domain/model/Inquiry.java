package com.back.catchmate.inquiry.domain.model;

import com.back.catchmate.global.authorization.common.ResourceOwnership;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.inquiry.domain.enums.InquiryType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry implements ResourceOwnership {
    private Long id;
    private Long userId;
    private InquiryType type;
    private String content;
    private String answer;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    public static Inquiry createInquiry(Long userId, InquiryType type, String content) {
        return Inquiry.builder()
                .userId(userId)
                .type(type)
                .content(content)
                .status(InquiryStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void registerAnswer(String answer) {
        if (this.status == InquiryStatus.ANSWERED) {
            throw new BaseException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
    }

    @Override
    public Long getOwnershipId() {
        return userId;
    }
}
