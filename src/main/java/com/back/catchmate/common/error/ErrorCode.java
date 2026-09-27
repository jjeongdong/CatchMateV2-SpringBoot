package com.back.catchmate.common.error;

import com.back.catchmate.global.error.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements com.back.catchmate.global.error.ErrorCode {

    // 게시글
    INVALID_ACCESS_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),

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
