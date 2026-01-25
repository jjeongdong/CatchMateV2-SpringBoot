package com.back.catchmate.domain.inquiry.model;

import com.back.catchmate.domain.user.model.User;
import inquiry.enums.InquiryType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Inquiry {
    private Long id;
    private User user;
    private InquiryType type;
    private String title;
    private String content;
    private String answer;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    public static Inquiry createInquiry(User user, InquiryType type, String title, String content) {
        return Inquiry.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .status(InquiryStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
