package com.back.catchmate.domain.inquiry.model;

import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.inquiry.enums.InquiryType;
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
    private User user;
    private InquiryType type;
    private String content;
    private String answer;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    public static Inquiry createInquiry(User user, InquiryType type, String content) {
        return Inquiry.builder()
                .user(user)
                .type(type)
                .content(content)
                .status(InquiryStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void registerAnswer(String answer) {
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
    }

    @Override
    public Long getOwnershipId() {
        return user.getId();
    }
}
