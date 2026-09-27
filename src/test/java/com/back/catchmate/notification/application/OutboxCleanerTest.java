package com.back.catchmate.notification.application;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxCleanerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 31, 4, 0);
    private static final LocalDateTime SUCCESS_THRESHOLD = LocalDateTime.of(2026, 1, 24, 4, 0);
    private static final LocalDateTime FAILURE_THRESHOLD = LocalDateTime.of(2026, 1, 1, 4, 0);
    private static final List<OutboxStatus> SUCCEEDED = List.of(OutboxStatus.SUCCESS);
    private static final List<OutboxStatus> FAILED = List.of(OutboxStatus.FAILED, OutboxStatus.PERMANENT_FAILURE);

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    private OutboxCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new OutboxCleaner(notificationOutboxRepository);
        ReflectionTestUtils.setField(cleaner, "successRetentionDays", 7);
        ReflectionTestUtils.setField(cleaner, "failureRetentionDays", 30);
        ReflectionTestUtils.setField(cleaner, "chunkSize", 1000);
    }

    @Test
    @DisplayName("성공 행은 7일, 실패 행은 30일이 지나면 지운다")
    void deletesByRetentionPerStatus() {
        // given
        given(notificationOutboxRepository.deleteFinishedBefore(eq(SUCCEEDED), eq(SUCCESS_THRESHOLD), anyInt()))
                .willReturn(0);
        given(notificationOutboxRepository.deleteFinishedBefore(eq(FAILED), eq(FAILURE_THRESHOLD), anyInt()))
                .willReturn(0);

        // when
        cleaner.deleteExpiredOutboxes(NOW);

        // then
        then(notificationOutboxRepository).should().deleteFinishedBefore(SUCCEEDED, SUCCESS_THRESHOLD, 1000);
        then(notificationOutboxRepository).should().deleteFinishedBefore(FAILED, FAILURE_THRESHOLD, 1000);
    }

    @Test
    @DisplayName("청크가 가득 차면 덜 찬 청크가 나올 때까지 이어서 지운다")
    void deletesInChunksUntilPartialChunk() {
        // given
        given(notificationOutboxRepository.deleteFinishedBefore(SUCCEEDED, SUCCESS_THRESHOLD, 1000))
                .willReturn(1000, 1000, 3);
        given(notificationOutboxRepository.deleteFinishedBefore(FAILED, FAILURE_THRESHOLD, 1000))
                .willReturn(0);

        // when
        cleaner.deleteExpiredOutboxes(NOW);

        // then
        then(notificationOutboxRepository).should(times(3)).deleteFinishedBefore(SUCCEEDED, SUCCESS_THRESHOLD, 1000);
        then(notificationOutboxRepository).should(times(1)).deleteFinishedBefore(FAILED, FAILURE_THRESHOLD, 1000);
    }
}
