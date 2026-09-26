package com.back.catchmate.chat.domain;

import com.back.catchmate.chat.domain.exception.ChatMessageTypeNotAllowedException;

public enum MessageType {
    TEXT, // 일반 텍스트 메시지
    SYSTEM; // 시스템 메시지 (입장, 퇴장 등) - 서버에서만 생성

    // 클라이언트가 SYSTEM 으로 보내면 입장·퇴장 안내를 사칭할 수 있다.
    public void verifySendableByUser() {
        if (this != TEXT) {
            throw new ChatMessageTypeNotAllowedException();
        }
    }
}
