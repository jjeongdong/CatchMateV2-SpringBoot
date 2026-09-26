package com.back.catchmate.chat.application.dto.result;

import com.back.catchmate.global.error.ErrorCode;

/**
 * STOMP 전송 실패를 발신자 세션에만 되돌려주는 에러 페이로드 (옛 ChatErrorResponse 와 같은 JSON).
 * HTTP 경로의 GlobalExceptionHandler 는 WebSocket 프레임에 적용되지 않아, 커밋 전 실패가
 * 로그만 남기고 삼켜지면 발신자는 전송 성공 여부를 알 수 없다. 이 응답이 그 통보 수단이다.
 * retryable: false = 도메인 규칙 위반(재전송해도 동일 실패), true = 일시 장애(재전송 유효).
 */
public record ChatErrorResult(Long chatRoomId, String code, String message, boolean retryable) {

    public static ChatErrorResult of(Long chatRoomId, ErrorCode errorCode, boolean retryable) {
        return of(chatRoomId, errorCode, errorCode.message(), retryable);
    }

    public static ChatErrorResult of(Long chatRoomId, ErrorCode errorCode, String message, boolean retryable) {
        return new ChatErrorResult(chatRoomId, errorCode.name(), message, retryable);
    }
}
