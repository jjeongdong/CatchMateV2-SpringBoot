package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.outbox;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxStateTransitionerTest {

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    private SimpleMeterRegistry meterRegistry;
    private OutboxStateTransitioner transitioner;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        transitioner = new OutboxStateTransitioner(meterRegistry, notificationOutboxRepository);
    }

    @Test
    @DisplayName("선점하면 PROCESSING 으로 바꿔 한 번의 배치로 반영한다")
    void claimsPending() {
        NotificationOutbox pending = outbox(1L, 2L, "{}");
        given(notificationOutboxRepository.findPendingForUpdate(5, 500)).willReturn(List.of(pending));

        List<NotificationOutbox> claimed = transitioner.claimPendingNotifications(5, 500);

        assertThat(claimed).containsExactly(pending);
        assertThat(pending.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        then(notificationOutboxRepository).should().updateAll(List.of(pending));
    }

    @Test
    @DisplayName("여러 수신자를 선점하면 PROCESSING 으로 바꿔 한 번의 배치로 반영한다")
    void claimsPendingByRecipientIds() {
        // given
        NotificationOutbox first = outbox(1L, 2L, "{}");
        NotificationOutbox second = outbox(2L, 3L, "{}");
        given(notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(2L, 3L), 500))
                .willReturn(List.of(first, second));

        // when
        List<NotificationOutbox> claimed = transitioner.claimPendingByRecipientIds(List.of(2L, 3L), 500);

        // then
        assertThat(claimed).containsExactly(first, second);
        assertThat(claimed).extracting(NotificationOutbox::getStatus).containsOnly(OutboxStatus.PROCESSING);
        then(notificationOutboxRepository).should().updateAll(List.of(first, second));
    }

    @Test
    @DisplayName("여러 수신자 선점 대상이 없으면 UPDATE 하지 않는다")
    void claimsNothingByRecipientIds() {
        // given
        given(notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(2L), 500))
                .willReturn(List.of());

        // when
        List<NotificationOutbox> claimed = transitioner.claimPendingByRecipientIds(List.of(2L), 500);

        // then
        assertThat(claimed).isEmpty();
        then(notificationOutboxRepository).should(never()).updateAll(any());
    }

    @Test
    @DisplayName("발송 결과 세 갈래를 한 번에 반영하고 지표를 올린다")
    void appliesDispatchResults() {
        NotificationOutbox success = outbox(1L, 2L, "{}");
        NotificationOutbox permanent = outbox(2L, 3L, "{}");
        NotificationOutbox retryable = outbox(3L, 4L, "{}");

        transitioner.applyDispatchResults(
                List.of(success), List.of(permanent), List.of(retryable), Map.of(2L, "토큰 만료", 3L, "일시 장애"), 1);

        assertThat(success.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(permanent.getStatus()).isEqualTo(OutboxStatus.PERMANENT_FAILURE);
        assertThat(retryable.getStatus()).isEqualTo(OutboxStatus.FAILED);
        then(notificationOutboxRepository).should().updateAll(List.of(success, permanent, retryable));
        assertThat(meterRegistry
                        .counter("notification.outbox.success", "retried", "false")
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry
                        .counter("notification.outbox.failure", "type", "permanent")
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry
                        .counter("notification.outbox.failure", "type", "max_retry_exceeded")
                        .count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("PROCESSING 에 정체된 행은 실패 1회로 계상해 재시도 대상으로 되돌린다")
    void recoversStuck() {
        NotificationOutbox stuck = outbox(1L, 2L, "{}");
        stuck.startProcessing();
        LocalDateTime threshold = LocalDateTime.of(2026, 9, 1, 12, 0);
        given(notificationOutboxRepository.findStuckProcessingForUpdate(threshold, 500))
                .willReturn(List.of(stuck));

        int recovered = transitioner.recoverStuckProcessing(threshold, 5, 500);

        assertThat(recovered).isEqualTo(1);
        assertThat(stuck.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stuck.getRetryCount()).isEqualTo(1);
        then(notificationOutboxRepository).should().save(stuck);
    }
}
