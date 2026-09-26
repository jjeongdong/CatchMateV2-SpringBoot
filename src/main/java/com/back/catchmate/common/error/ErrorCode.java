package com.back.catchmate.common.error;

import com.back.catchmate.global.error.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements com.back.catchmate.global.error.ErrorCode {

    // 유저
    FORBIDDEN_ACCESS(ErrorType.FORBIDDEN, "접근 권한이 없습니다."),

    // 신청
    ALREADY_ENROLL_PENDING(ErrorType.INVALID, "이미 신청 대기 중인 게시글입니다."),
    ALREADY_ENROLL_REJECTED(ErrorType.INVALID, "이미 거절된 신청 내역이 있어 재신청할 수 없습니다."),
    ALREADY_ENROLL_ACCEPTED(ErrorType.INVALID, "이미 수락된 신청 내역이 있습니다."),
    ENROLL_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 직관 신청입니다."),
    ENROLL_BAD_REQUEST(ErrorType.INVALID, "자신의 게시글에는 직관 신청을 할 수 없습니다."),
    DUPLICATE_ENROLL_ACCEPT_REQUEST(ErrorType.INVALID, "이미 처리 중인 수락 요청입니다."),
    ENROLL_ACCEPT_CONFLICT(ErrorType.CONFLICT, "동시 요청이 많아 수락 처리에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // 게시글
    BOARD_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 게시글입니다."),
    TEMP_BOARD_NOT_FOUND(ErrorType.NOT_FOUND, "임시 저장된 글이 존재하지 않습니다."),
    TEMP_BOARD_BAD_REQUEST(ErrorType.NOT_FOUND, "임시 저장된 글을 불러올 권한이 없습니다."),
    INVALID_ACCESS_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    ALREADY_BOOKMARK(ErrorType.INVALID, "이미 찜한 게시글입니다."),
    BOOKMARK_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 찜입니다."),
    BOOKMARK_BAD_REQUEST(ErrorType.INVALID, "본인 게시글은 찜할 수 없습니다."),
    FULL_PERSON(ErrorType.INVALID, "해당 게시글은 마감되었습니다."),
    BOARD_CANNOT_UPDATE_AFTER_ENROLL(ErrorType.INVALID, "참여 인원이 존재하여 게시글을 수정할 수 없습니다."),
    BOARD_TITLE_MISSING(ErrorType.INVALID, "게시글 제목은 필수입니다."),
    BOARD_CONTENT_MISSING(ErrorType.INVALID, "게시글 내용은 필수입니다."),
    BOARD_MAX_PERSON_MISSING(ErrorType.INVALID, "모집 인원은 필수입니다."),
    BOARD_CHEER_CLUB_MISSING(ErrorType.INVALID, "응원 구단 선택은 필수입니다."),
    BOARD_GAME_MISSING(ErrorType.INVALID, "직관할 경기 선택은 필수입니다."),

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

    // 유저 차단
    BLOCKED_USER_BOARD(ErrorType.INVALID, "내가 차단한 유저의 게시글입니다."),

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
