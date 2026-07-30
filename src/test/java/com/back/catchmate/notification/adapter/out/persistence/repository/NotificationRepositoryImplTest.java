package com.back.catchmate.notification.adapter.out.persistence.repository;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.notification.application.port.out.persistence.NotificationRepository;
import com.back.catchmate.notification.domain.model.AlarmType;
import com.back.catchmate.notification.domain.model.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code saveAll} 은 JPA 가 아니라 손으로 쓴 멀티로우 INSERT 다(IDENTITY 라 Hibernate batch 불가).
 * 컬럼명·개수·순서가 틀려도 문자열이라 컴파일에서 안 잡히고, 단위 테스트는 리포지토리를 모킹하므로 여기까지 오지 않는다.
 * 그래서 실제 스키마에 넣어보고 <b>JPA 매핑으로 되읽어</b> 둘이 같은 컬럼을 가리키는지 확인한다.
 * <p>
 * {@code notifications.user_id} 는 {@code users} 를 참조하는 FK 라 임의의 id 로는 적재할 수 없다.
 * 그래서 기존 사용자를 빌려 쓰고, 정리는 <b>이 테스트가 만든 행만</b> 지우도록 전용 targetId 로 한정한다.
 */
@SpringBootTest(classes = CatchmateApplication.class, properties = "spring.profiles.active=local")
class NotificationRepositoryImplTest {

    // 실제 사용자의 알림과 섞이지 않도록, 이 테스트가 만든 행을 식별하는 전용 값.
    private static final Long NOTICE_TARGET_ID = 990_000_777L;
    private static final Long ENROLL_TARGET_ID = 990_000_888L;

    @Autowired
    private NotificationRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long existingUserId;

    @BeforeEach
    void resolveExistingUser() {
        existingUserId = jdbcTemplate.query(
                "SELECT user_id FROM users LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        assumeTrue(existingUserId != null, "notifications.user_id 가 users FK 라 기존 사용자가 최소 1명 필요하다");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notifications WHERE target_id IN (?, ?)",
                NOTICE_TARGET_ID, ENROLL_TARGET_ID);
    }

    @Test
    @DisplayName("saveAll 로 적재한 알림을 되읽으면 모든 필드가 그대로 보존된다")
    void saveAll_적재한_필드가_그대로_보존된다() {
        // given - 공지 형태(senderId 가 null) 와 발신자가 있는 형태를 함께 넣어 nullable 컬럼의 양쪽 분기를 태운다.
        // sender_id 도 users FK 라 임의 값을 못 쓰고, board_id 는 boards FK 라 null 로 둔다.
        Notification notice = Notification.createNotification(
                existingUserId, null, null, "공지 제목", AlarmType.EVENT, NOTICE_TARGET_ID);
        Notification enroll = Notification.createNotification(
                existingUserId, existingUserId, null, "신청 제목", AlarmType.ENROLL, ENROLL_TARGET_ID);

        // when
        sut.saveAll(List.of(notice, enroll));

        // then
        Notification savedNotice = findByTargetId(NOTICE_TARGET_ID);
        assertThat(savedNotice.getUserId()).isEqualTo(existingUserId);
        assertThat(savedNotice.getTitle()).isEqualTo("공지 제목");
        assertThat(savedNotice.getType()).isEqualTo(AlarmType.EVENT);
        assertThat(savedNotice.isRead()).isFalse();
        assertThat(savedNotice.getSenderId()).isNull();      // setNullableLong 경로
        assertThat(savedNotice.getBoardId()).isNull();
        assertThat(savedNotice.getCreatedAt()).isNotNull();  // JPA 감사 대신 SQL 로 직접 채우는 컬럼

        Notification savedEnroll = findByTargetId(ENROLL_TARGET_ID);
        assertThat(savedEnroll.getTitle()).isEqualTo("신청 제목");
        assertThat(savedEnroll.getType()).isEqualTo(AlarmType.ENROLL);
        assertThat(savedEnroll.getSenderId()).isEqualTo(existingUserId);  // setNullableLong 의 non-null 분기
    }

    @Test
    @DisplayName("saveAll 에 빈 목록을 넘기면 아무것도 적재하지 않는다")
    void saveAll_빈_목록이면_적재하지_않는다() {
        // when
        sut.saveAll(List.of());

        // then
        assertThat(countTestRows()).isZero();
    }

    /** JPA 매핑으로 되읽어 raw INSERT 가 쓴 컬럼과 엔티티가 읽는 컬럼이 같은지 확인한다. */
    private Notification findByTargetId(Long targetId) {
        return sut.findAllByUserId(existingUserId, PageRequest.of(0, 100)).getContent().stream()
                .filter(notification -> Objects.equals(notification.getTargetId(), targetId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("targetId=" + targetId + " 인 알림이 적재되지 않았다"));
    }

    private Integer countTestRows() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE target_id IN (?, ?)",
                Integer.class, NOTICE_TARGET_ID, ENROLL_TARGET_ID);
    }
}
