package com.back.catchmate.notification.domain;

/**
 * 푸시 1건의 결과.
 * <p>
 * {@code permanentFailure} 는 토큰 만료·잘못된 인자처럼 재시도해도 결과가 바뀌지 않는 실패다.
 * 그 외 실패는 아웃박스가 PENDING 으로 되돌려 다음 스케줄러 주기에 다시 시도한다.
 */
public record PushOutcome(boolean success, boolean permanentFailure, String errorMessage) {

    public static PushOutcome ofSuccess() {
        return new PushOutcome(true, false, null);
    }

    public static PushOutcome ofPermanentFailure(String errorMessage) {
        return new PushOutcome(false, true, errorMessage);
    }

    public static PushOutcome ofRetryableFailure(String errorMessage) {
        return new PushOutcome(false, false, errorMessage);
    }
}
