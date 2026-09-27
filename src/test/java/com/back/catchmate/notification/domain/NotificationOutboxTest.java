package com.back.catchmate.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationOutboxTest {

    @Test
    @DisplayName("새 아웃박스는 PENDING, 재시도 0회다")
    void createsPending() {
        NotificationOutbox outbox = NotificationOutbox.create(1L, "token", "제목", "본문", "{}");

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
        assertThat(outbox.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("재시도 가능한 실패는 한도 전이면 PENDING 으로 돌아가고 사유를 남긴다")
    void failRetryableBelowLimit() {
        NotificationOutbox outbox = NotificationOutbox.create(1L, "token", "제목", "본문", "{}");
        outbox.startProcessing();

        outbox.failRetryable("일시 장애", 5);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getErrorMessage()).isEqualTo("일시 장애");
        assertThat(outbox.isFailed()).isFalse();
    }

    @Test
    @DisplayName("재시도 한도에 닿으면 FAILED 로 멈춘다")
    void failRetryableAtLimit() {
        NotificationOutbox outbox = NotificationOutbox.create(1L, "token", "제목", "본문", "{}");

        outbox.failRetryable("1", 2);
        outbox.failRetryable("2", 2);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(2);
        assertThat(outbox.isFailed()).isTrue();
    }

    @Test
    @DisplayName("성공·영구 실패 전이")
    void successAndPermanentFailure() {
        NotificationOutbox succeeded = NotificationOutbox.create(1L, "token", "제목", "본문", "{}");
        NotificationOutbox failed = NotificationOutbox.create(1L, "token", "제목", "본문", "{}");

        succeeded.success();
        failed.permanentFail("토큰 만료");

        assertThat(succeeded.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.PERMANENT_FAILURE);
        assertThat(failed.getErrorMessage()).isEqualTo("토큰 만료");
    }
}
