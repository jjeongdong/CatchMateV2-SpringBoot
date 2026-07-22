package com.back.catchmate.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 클럽
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 구단입니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게임입니다."),

    // 신청
    ALREADY_ENROLL_PENDING(HttpStatus.BAD_REQUEST, "이미 신청 대기 중인 게시글입니다."),
    ALREADY_ENROLL_REJECTED(HttpStatus.BAD_REQUEST, "이미 거절된 신청 내역이 있어 재신청할 수 없습니다."),
    ALREADY_ENROLL_ACCEPTED(HttpStatus.BAD_REQUEST, "이미 수락된 신청 내역이 있습니다."),
    ENROLL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 직관 신청입니다."),
    ENROLL_BAD_REQUEST(HttpStatus.BAD_REQUEST, "자신의 게시글에는 직관 신청을 할 수 없습니다."),
    DUPLICATE_ENROLL_ACCEPT_REQUEST(HttpStatus.BAD_REQUEST, "이미 처리 중인 수락 요청입니다."),
    ENROLL_ACCEPT_CONFLICT(HttpStatus.CONFLICT, "동시 요청이 많아 수락 처리에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // 게시글
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    TEMP_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "임시 저장된 글이 존재하지 않습니다."),
    TEMP_BOARD_BAD_REQUEST(HttpStatus.NOT_FOUND, "임시 저장된 글을 불러올 권한이 없습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    ALREADY_BOOKMARK(HttpStatus.BAD_REQUEST, "이미 찜한 게시글입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 찜입니다."),
    BOOKMARK_BAD_REQUEST(HttpStatus.BAD_REQUEST, "본인 게시글은 찜할 수 없습니다."),
    FULL_PERSON(HttpStatus.BAD_REQUEST, "해당 게시글은 마감되었습니다."),
    BOARD_CANNOT_UPDATE_AFTER_ENROLL(HttpStatus.BAD_REQUEST, "참여 인원이 존재하여 게시글을 수정할 수 없습니다."),
    BOARD_TITLE_MISSING(HttpStatus.BAD_REQUEST, "게시글 제목은 필수입니다."),
    BOARD_CONTENT_MISSING(HttpStatus.BAD_REQUEST, "게시글 내용은 필수입니다."),
    BOARD_MAX_PERSON_MISSING(HttpStatus.BAD_REQUEST, "모집 인원은 필수입니다."),
    BOARD_CHEER_CLUB_MISSING(HttpStatus.BAD_REQUEST, "응원 구단 선택은 필수입니다."),
    BOARD_GAME_MISSING(HttpStatus.BAD_REQUEST, "직관할 경기 선택은 필수입니다."),

    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_OUTBOX_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 아웃박스 저장에 실패했습니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 알림 전송에 실패했습니다."),

    // 채팅방
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHATROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방 멤버를 찾을 수 없습니다."),
    USER_CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자가 해당 채팅방에 참여하지 않았습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅 메시지입니다."),
    CHATROOM_REENTRY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이미 퇴장한 채팅방에는 다시 입장할 수 없습니다."),
    CHATROOM_READ_ONLY(HttpStatus.FORBIDDEN, "차단으로 인해 읽기 전용 상태인 채팅방입니다."),

    // 유저 차단
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 차단 내역입니다."),
    ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "해당 유저를 이미 차단했습니다."),
    SELF_BLOCK_FAILED(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),
    BLOCKED_USER_BOARD(HttpStatus.BAD_REQUEST, "내가 차단한 유저의 게시글입니다."),

    // 문의
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변이 등록된 문의는 수정할 수 없습니다."),

    // 신고
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다."),
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 신고할 수 없습니다."),

    // 소켓
    SOCKET_CONNECT_FAILED(HttpStatus.UNAUTHORIZED, "소켓 연결에 실패했습니다."),

    // 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "클라이언트 오류입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다."),

    // OAuth
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth 공급자 통신 중 오류가 발생했습니다."),
    OAUTH_STATE_MISMATCH(HttpStatus.BAD_REQUEST, "OAuth state 검증에 실패했습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 공급자입니다."),
    INVALID_SIGNUP_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 회원가입 토큰입니다."),
    MISSING_REFRESH_COOKIE(HttpStatus.UNAUTHORIZED, "Refresh Token 쿠키가 존재하지 않습니다."),

    // 공지글
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공지입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
