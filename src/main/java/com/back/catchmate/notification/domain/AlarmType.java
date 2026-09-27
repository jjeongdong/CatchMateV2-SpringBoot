package com.back.catchmate.notification.domain;

public enum AlarmType {
    ALL("전체"),
    CHAT("채팅"),
    ENROLL("신청"),
    EVENT("이벤트"),
    INQUIRY_ANSWER("1:1 문의 답변");

    private final String description;

    AlarmType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
