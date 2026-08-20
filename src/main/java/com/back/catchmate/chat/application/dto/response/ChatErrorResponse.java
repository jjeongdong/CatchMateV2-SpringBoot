package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.common.error.ErrorCode;

/**
 * STOMP 전송 실패를 발신자 세션에만 되돌려주는 에러 페이로드.
 * <p>HTTP 경로의 GlobalExceptionHandler 는 WebSocket 프레임에 적용되지 않아, 커밋 전 실패가
 * 로그만 남기고 삼켜지면 발신자는 전송 성공 여부를 알 수 없다. 이 응답이 그 통보 수단이다.
 * <p>{@code retryable} 은 재전송 가치가 있는지를 뜻한다.
 * false = 도메인 규칙 위반(읽기 전용 방·비참여자 등, 재전송해도 동일 실패),
 * true = 일시 장애(Redis·DB 등, 재전송 유효).
 */
public record ChatErrorResponse(
        Long chatRoomId,
        String code,
        String message,
        boolean retryable
) {
    public static ChatErrorResponse of(Long chatRoomId, ErrorCode errorCode, boolean retryable) {
        return of(chatRoomId, errorCode, errorCode.getMessage(), retryable);
    }

    public static ChatErrorResponse of(Long chatRoomId, ErrorCode errorCode, String message, boolean retryable) {
        return new ChatErrorResponse(chatRoomId, errorCode.name(), message, retryable);
    }
}
