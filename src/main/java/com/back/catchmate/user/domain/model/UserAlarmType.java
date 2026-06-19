package com.back.catchmate.user.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserAlarmType {
    ALL("전체"),
    CHAT("채팅"),
    ENROLL("신청"),
    EVENT("이벤트");

    @Getter
    private final String description;
}
