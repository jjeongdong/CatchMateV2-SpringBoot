package com.back.catchmate.notifications.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceType {
    CHAT_MESSAGE("채팅 메시지"),
    ENROLL("참여 요청"),
    ADMIN_INQUIRY_ANSWER("관리자 문의 답변");

    private final String description;
}
