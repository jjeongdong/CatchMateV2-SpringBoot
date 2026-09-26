package com.back.catchmate.chat.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum ChatErrorCode implements ErrorCode {
    CHAT_ROOM_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHAT_ROOM_MEMBER_NOT_FOUND(ErrorType.NOT_FOUND, "채팅방 멤버를 찾을 수 없습니다."),
    CHAT_ROOM_REENTRY_NOT_ALLOWED(ErrorType.CONFLICT, "이미 퇴장한 채팅방에는 다시 입장할 수 없습니다."),
    CHAT_ROOM_READ_ONLY(ErrorType.FORBIDDEN, "차단으로 인해 읽기 전용 상태인 채팅방입니다."),
    CHAT_ROOM_NOT_HOST(ErrorType.FORBIDDEN, "방장만 참여자를 내보낼 수 있습니다."),
    CHAT_SELF_KICK_NOT_ALLOWED(ErrorType.INVALID, "자기 자신은 내보낼 수 없습니다."),
    CHAT_MESSAGE_TYPE_NOT_ALLOWED(ErrorType.INVALID, "보낼 수 없는 메시지 종류입니다."),
    CHAT_SENDER_UNAUTHENTICATED(ErrorType.UNAUTHORIZED, "인증되지 않은 채팅 요청입니다."),
    CHAT_SUBSCRIPTION_DESTINATION_INVALID(ErrorType.INVALID, "잘못된 채팅방 구독 주소입니다."),
    CHAT_CURSOR_INVALID(ErrorType.INVALID, "잘못된 커서입니다.");

    private final ErrorType type;
    private final String message;

    ChatErrorCode(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}
