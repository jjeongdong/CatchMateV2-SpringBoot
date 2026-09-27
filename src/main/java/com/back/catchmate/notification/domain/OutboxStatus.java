package com.back.catchmate.notification.domain;

public enum OutboxStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    SUCCESS("전송 성공"),
    FAILED("전송 실패"),
    PERMANENT_FAILURE("영구 실패 - 재시도 불가");

    private final String description;

    OutboxStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
