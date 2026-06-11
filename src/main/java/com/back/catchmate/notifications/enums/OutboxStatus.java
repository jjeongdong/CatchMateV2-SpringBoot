package com.back.catchmate.notifications.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    SUCCESS("전송 성공"),
    FAILED("전송 실패");

    private final String description;
}
