package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import com.back.catchmate.notifications.enums.NotificationChannel;
import com.back.catchmate.notifications.enums.OutboxStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxUpdaterTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private NotificationOutboxUpdater outboxUpdater;

    @Test
    @DisplayName("실패 시 에러 메시지가 정상적으로 기록되어야 한다")
    void updateStatusFailure_recordsErrorMessage() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(
                1L, "token", NotificationChannel.FCM, "title", "body", "{}"
        );
        int maxRetryCount = 5;
        String errorMsg = "FCM connection failed";

        // when
        outboxUpdater.updateStatusFailure(outbox, maxRetryCount, errorMsg);

        // then
        assertThat(outbox.getErrorMessage()).isEqualTo(errorMsg);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxRepository).save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 상태가 FAILED로 변경되어야 한다")
    void updateStatusFailure_maxRetryExceeded() {
        // given
        NotificationOutbox outbox = NotificationOutbox.builder()
                .retryCount(4)
                .status(OutboxStatus.PROCESSING)
                .build();
        int maxRetryCount = 5;
        String errorMsg = "Final failure";

        Counter mockCounter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(mockCounter);

        // when
        outboxUpdater.updateStatusFailure(outbox, maxRetryCount, errorMsg);

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(5);
        assertThat(outbox.getErrorMessage()).isEqualTo(errorMsg);
        verify(mockCounter).increment();
    }
}
