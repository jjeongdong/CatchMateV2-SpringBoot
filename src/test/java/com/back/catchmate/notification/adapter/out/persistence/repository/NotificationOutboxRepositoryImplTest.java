package com.back.catchmate.notification.adapter.out.persistence.repository;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.domain.model.OutboxStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code updateAll} 은 손으로 쓴 배치 UPDATE 이고, 이제 <b>모든 알림</b>의 선점·상태 확정이 이 문장을 지난다.
 * 틀리면 알림 발송이 통째로 멈추므로 실제 스키마에 대고 확인한다.
 * <p>
 * id 회수를 {@code findAllPendingByRecipientId} 로 하지 않는 이유: 그 쿼리는
 * {@code @Lock(PESSIMISTIC_WRITE)} 라 트랜잭션을 요구하고, 같은 영속성 컨텍스트에서 되읽으면
 * 1차 캐시 때문에 UPDATE 반영 여부를 확인할 수 없다. 그래서 id 는 JDBC 로 뽑고 검증은 JPA 로 되읽는다.
 */
@SpringBootTest(classes = CatchmateApplication.class, properties = "spring.profiles.active=local")
class NotificationOutboxRepositoryImplTest {

    // 운영/다른 테스트 데이터와 겹치지 않도록 전용 식별자를 쓴다.
    private static final Long TEST_RECIPIENT_ID = 990_000_002L;
    private static final String TEST_PAYLOAD = "{\"type\":\"EVENT\"}";

    @Autowired
    private NotificationOutboxRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification_outbox WHERE recipient_id = ?", TEST_RECIPIENT_ID);
    }

    @Test
    @DisplayName("updateAll 은 성공·재시도 상태 전이를 한 번에 반영한다")
    void updateAll_상태전이가_반영된다() {
        // given - 두 건을 적재하고, 스케줄러가 하듯 성공 1건 / 재시도 1건으로 전이시킨다
        List<Long> ids = saveOutboxes(2);

        NotificationOutbox succeeded = loadedOutbox(ids.get(0));
        succeeded.success();

        NotificationOutbox retryable = loadedOutbox(ids.get(1));
        retryable.incrementRetryCount();
        retryable.recordError("FCM 일시적 오류");
        retryable.pending();

        // when
        sut.updateAll(List.of(succeeded, retryable));

        // then
        NotificationOutbox reloadedSuccess = sut.findById(ids.get(0)).orElseThrow();
        assertThat(reloadedSuccess.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(reloadedSuccess.getRetryCount()).isZero();

        NotificationOutbox reloadedRetryable = sut.findById(ids.get(1)).orElseThrow();
        assertThat(reloadedRetryable.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(reloadedRetryable.getRetryCount()).isEqualTo(1);
        assertThat(reloadedRetryable.getErrorMessage()).isEqualTo("FCM 일시적 오류");
    }

    @Test
    @DisplayName("updateAll 은 상태 컬럼만 바꾸고 수신자·본문·payload 는 건드리지 않는다")
    void updateAll_불변_컬럼은_유지된다() {
        // given
        Long id = saveOutboxes(1).get(0);
        NotificationOutbox claimed = loadedOutbox(id);
        claimed.startProcessing();

        // when
        sut.updateAll(List.of(claimed));

        // then
        NotificationOutbox reloaded = sut.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(reloaded.getRecipientId()).isEqualTo(TEST_RECIPIENT_ID);
        assertThat(reloaded.getRecipientAddress()).isEqualTo("token-" + id);
        assertThat(reloaded.getTitle()).isEqualTo("title");
        assertThat(reloaded.getBody()).isEqualTo("body");
        assertThat(reloaded.getPayload()).isEqualTo(TEST_PAYLOAD);
    }

    @Test
    @DisplayName("updateAll 에 빈 목록을 넘겨도 예외 없이 통과한다")
    void updateAll_빈_목록이면_아무_일도_없다() {
        // when & then - 발송 대상이 전부 걸러진 배치에서 실제로 발생하는 입력이다
        sut.updateAll(List.of());
    }

    /** 적재 후 id 를 회수한다. 멀티로우 INSERT 라 적재 시점엔 id 를 알 수 없다. */
    private List<Long> saveOutboxes(int count) {
        List<NotificationOutbox> outboxes = IntStream.range(0, count)
                .mapToObj(i -> NotificationOutbox.create(
                        TEST_RECIPIENT_ID, "placeholder", "title", "body", TEST_PAYLOAD))
                .toList();
        sut.saveAll(outboxes);

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM notification_outbox WHERE recipient_id = ? ORDER BY id",
                Long.class, TEST_RECIPIENT_ID);
        assertThat(ids).hasSize(count);

        // 불변 컬럼 검증에서 행마다 구분되도록 토큰을 id 기반으로 확정한다.
        ids.forEach(id -> jdbcTemplate.update(
                "UPDATE notification_outbox SET fcm_token = ? WHERE id = ?", "token-" + id, id));
        return ids;
    }

    /** 스케줄러가 {@code findAllPending} 으로 읽어온 직후의 도메인 객체와 같은 상태를 만든다. */
    private NotificationOutbox loadedOutbox(Long id) {
        return NotificationOutbox.builder()
                .id(id)
                .recipientId(TEST_RECIPIENT_ID)
                .recipientAddress("token-" + id)
                .title("title")
                .body("body")
                .payload(TEST_PAYLOAD)
                .retryCount(0)
                .status(OutboxStatus.PROCESSING)
                .build();
    }
}
