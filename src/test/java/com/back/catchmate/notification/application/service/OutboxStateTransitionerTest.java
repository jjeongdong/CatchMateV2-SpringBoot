package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.model.OutboxStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxStateTransitionerTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private OutboxStateTransitioner outboxStateTransitioner;

    @Test
    @DisplayName("실패 시 에러 메시지가 정상적으로 기록되어야 한다")
    void updateStatusFailure_recordsErrorMessage() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(
                1L, "token", "title", "body", "{}"
        );
        int maxRetryCount = 5;
        String errorMsg = "FCM connection failed";

        // when
        outboxStateTransitioner.updateStatusFailure(outbox, maxRetryCount, errorMsg);

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
        outboxStateTransitioner.updateStatusFailure(outbox, maxRetryCount, errorMsg);

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(5);
        assertThat(outbox.getErrorMessage()).isEqualTo(errorMsg);
        verify(mockCounter).increment();
    }

    @Test
    @DisplayName("PROCESSING 상태로 정체된 알림은 재시도 1회를 계상하고 PENDING으로 되돌린다")
    void recoverStuckProcessing_returnsToPending() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        NotificationOutbox stuck = NotificationOutbox.builder()
                .retryCount(0)
                .status(OutboxStatus.PROCESSING)
                .build();
        when(outboxRepository.findAllStuckProcessing(threshold, 50)).thenReturn(List.of(stuck));

        Counter mockCounter = mock(Counter.class);
        when(meterRegistry.counter("notification.outbox.recovered")).thenReturn(mockCounter);

        // when
        int recovered = outboxStateTransitioner.recoverStuckProcessing(threshold, 5, 50);

        // then
        assertThat(recovered).isEqualTo(1);
        assertThat(stuck.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stuck.getRetryCount()).isEqualTo(1);
        assertThat(stuck.getErrorMessage()).isEqualTo("PROCESSING 상태 정체로 회수됨");
        verify(outboxRepository).save(stuck);
        verify(mockCounter).increment();
    }

    @Test
    @DisplayName("정체된 알림이 이미 최대 재시도에 도달했으면 회수하면서 FAILED로 확정한다")
    void recoverStuckProcessing_maxRetryExceeded() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        NotificationOutbox stuck = NotificationOutbox.builder()
                .retryCount(4)
                .status(OutboxStatus.PROCESSING)
                .build();
        when(outboxRepository.findAllStuckProcessing(threshold, 50)).thenReturn(List.of(stuck));

        Counter mockCounter = mock(Counter.class);
        when(meterRegistry.counter(anyString())).thenReturn(mockCounter);
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(mockCounter);

        // when
        outboxStateTransitioner.recoverStuckProcessing(threshold, 5, 50);

        // then
        assertThat(stuck.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(stuck.getRetryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("정체된 알림이 없으면 아무것도 저장하지 않는다")
    void recoverStuckProcessing_noStuckRows() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        when(outboxRepository.findAllStuckProcessing(threshold, 50)).thenReturn(List.of());

        // when
        int recovered = outboxStateTransitioner.recoverStuckProcessing(threshold, 5, 50);

        // then
        assertThat(recovered).isZero();
        verify(outboxRepository, never()).save(any());
        verifyNoInteractions(meterRegistry);
    }
}
