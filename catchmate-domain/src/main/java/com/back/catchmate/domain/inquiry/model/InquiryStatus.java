package com.back.catchmate.domain.inquiry.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum InquiryStatus {
    WAITING("답변 대기"),
    ANSWERED("답변 완료");

    @Getter
    private final String description;
}
