package com.back.catchmate.inquiry.domain;

public enum InquiryStatus {
    WAITING("답변 대기"),
    ANSWERED("답변 완료");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
