package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 채팅 메시지마다 오프라인 수신자 수만큼 행이 쌓이므로, 처리가 끝난 행은 보관 기간이 지나면 지운다.
// 트랜잭션을 걸지 않는다 — 청크마다 DELETE 가 따로 커밋돼야 잠금과 복제 지연이 청크 크기로 묶인다.
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxCleaner {
    private static final List<OutboxStatus> SUCCEEDED = List.of(OutboxStatus.SUCCESS);
    // 실패는 원인 조사용으로 더 오래 둔다.
    private static final List<OutboxStatus> FAILED = List.of(OutboxStatus.FAILED, OutboxStatus.PERMANENT_FAILURE);

    private final NotificationOutboxRepository notificationOutboxRepository;

    @Value("${notification.outbox.cleanup.success-retention-days:7}")
    private int successRetentionDays;

    @Value("${notification.outbox.cleanup.failure-retention-days:30}")
    private int failureRetentionDays;

    @Value("${notification.outbox.cleanup.chunk-size:1000}")
    private int chunkSize;

    public void deleteExpiredOutboxes(LocalDateTime now) {
        int succeededCount = deleteInChunks(SUCCEEDED, now.minusDays(successRetentionDays));
        int failedCount = deleteInChunks(FAILED, now.minusDays(failureRetentionDays));
        log.info("보관 기간이 지난 아웃박스 정리 완료 succeededCount={}, failedCount={}", succeededCount, failedCount);
    }

    // 기준 시각이 고정이라 새로 끝나는 행은 대상이 아니어서, 덜 찬 청크가 나오면 끝난다.
    private int deleteInChunks(List<OutboxStatus> statuses, LocalDateTime threshold) {
        int totalCount = 0;
        int deletedCount;
        do {
            deletedCount = notificationOutboxRepository.deleteFinishedBefore(statuses, threshold, chunkSize);
            totalCount += deletedCount;
        } while (deletedCount == chunkSize);
        return totalCount;
    }
}
