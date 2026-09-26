package com.back.catchmate.common.error;

import com.back.catchmate.global.error.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements com.back.catchmate.global.error.ErrorCode {

    // 유저
    FORBIDDEN_ACCESS(ErrorType.FORBIDDEN, "접근 권한이 없습니다."),

    // 게시글
    INVALID_ACCESS_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),

    // 알림
    NOTIFICATION_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_OUTBOX_SAVE_FAILED(ErrorType.INTERNAL, "알림 아웃박스 저장에 실패했습니다."),
    FCM_SEND_FAILED(ErrorType.INTERNAL, "FCM 알림 전송에 실패했습니다."),

    // 채팅방
    CHATROOM_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHATROOM_MEMBER_NOT_FOUND(ErrorType.NOT_FOUND, "채팅방 멤버를 찾을 수 없습니다."),
    USER_CHATROOM_NOT_FOUND(ErrorType.NOT_FOUND, "사용자가 해당 채팅방에 참여하지 않았습니다."),
    CHAT_MESSAGE_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 채팅 메시지입니다."),
    CHATROOM_REENTRY_NOT_ALLOWED(ErrorType.INVALID, "이미 퇴장한 채팅방에는 다시 입장할 수 없습니다."),
    CHATROOM_READ_ONLY(ErrorType.FORBIDDEN, "차단으로 인해 읽기 전용 상태인 채팅방입니다."),

    // 소켓
    SOCKET_CONNECT_FAILED(ErrorType.UNAUTHORIZED, "소켓 연결에 실패했습니다."),

    // 토큰
    BAD_REQUEST(ErrorType.INVALID, "클라이언트 오류입니다."),
    INTERNAL_SERVER_ERROR(ErrorType.INTERNAL, "서버 오류입니다.");

    private final ErrorType type;
    private final String message;

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}
