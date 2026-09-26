package com.back.catchmate.board.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum BoardErrorCode implements ErrorCode {
    BOARD_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 게시글입니다."),
    BOARD_NOT_WRITER(ErrorType.FORBIDDEN, "본인이 작성한 게시글만 변경할 수 있습니다."),
    BOARD_FULL(ErrorType.CONFLICT, "해당 게시글은 마감되었습니다."),
    BOARD_NOT_EDITABLE_AFTER_ENROLL(ErrorType.CONFLICT, "참여 인원이 존재하여 게시글을 수정할 수 없습니다."),
    BOARD_TITLE_MISSING(ErrorType.INVALID, "게시글 제목은 필수입니다."),
    BOARD_CONTENT_MISSING(ErrorType.INVALID, "게시글 내용은 필수입니다."),
    BOARD_CHEER_CLUB_MISSING(ErrorType.INVALID, "응원 구단 선택은 필수입니다."),
    BOARD_GAME_MISSING(ErrorType.INVALID, "직관할 경기 선택은 필수입니다."),
    BOARD_WRITER_BLOCKED(ErrorType.INVALID, "내가 차단한 유저의 게시글입니다."),
    BOARD_CURSOR_INVALID(ErrorType.INVALID, "잘못된 커서입니다.");

    private final ErrorType type;
    private final String message;

    BoardErrorCode(ErrorType type, String message) {
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
