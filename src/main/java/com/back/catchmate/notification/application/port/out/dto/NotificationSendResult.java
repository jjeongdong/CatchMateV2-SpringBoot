package com.back.catchmate.notification.application.port.out.dto;

/**
 * 배치 발송 1건의 결과.
 * <p>
 * {@code permanentFailure} 는 토큰 만료·잘못된 인자처럼 재시도해도 결과가 바뀌지 않는 실패를 뜻한다.
 * 그 외 실패는 재시도 대상이며, 호출자(아웃박스)가 PENDING 으로 되돌려 다음 스케줄러 주기에 다시 시도한다.
 */
public record NotificationSendResult(boolean success, boolean permanentFailure, String errorMessage) {

    public static NotificationSendResult ofSuccess() {
        return new NotificationSendResult(true, false, null);
    }

    public static NotificationSendResult ofPermanentFailure(String errorMessage) {
        return new NotificationSendResult(false, true, errorMessage);
    }

    public static NotificationSendResult ofRetryableFailure(String errorMessage) {
        return new NotificationSendResult(false, false, errorMessage);
    }
}
