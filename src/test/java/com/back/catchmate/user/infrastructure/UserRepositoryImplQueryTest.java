package com.back.catchmate.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserAlarmType;
import com.back.catchmate.user.fixture.UserFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// application.yml 의 기본 프로필 dev 는 운영 RDS 를 가리킨다. create-drop 이 운영 DB 에 닿지 않도록
// 존재하지 않는 test 프로필로 dev 설정을 끄고, datasource 는 컨테이너(@ServiceConnection)에서만 받는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, UserRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User persist(String providerId, Long clubId) {
        return entityManager.persist(UserFixture.newUser(providerId, clubId));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("구단별 페이징 조회 - 구단으로 거르고 최신 가입순으로 offset/limit 을 적용한다")
    void filtersSortsAndPages() {
        // given
        User first = persist("p1", 1L);
        User second = persist("p2", 1L);
        persist("p3", 2L);
        User fourth = persist("p4", 1L);
        flushAndClear();

        // when
        List<User> firstPage = userRepository.findAllByClubId(1L, 0, 2);
        List<User> secondPage = userRepository.findAllByClubId(1L, 2, 2);

        // then
        assertThat(firstPage).extracting(User::getId).containsExactly(fourth.getId(), second.getId());
        assertThat(secondPage).extracting(User::getId).containsExactly(first.getId());
        assertThat(userRepository.countByClubId(1L)).isEqualTo(3);
    }

    @Test
    @DisplayName("구단별 페이징 조회 - 구단이 null 이면 전체를 대상으로 한다")
    void nullClubMeansAll() {
        // given
        persist("p1", 1L);
        persist("p2", 2L);
        flushAndClear();

        // when & then
        assertThat(userRepository.findAllByClubId(null, 0, 10)).hasSize(2);
        assertThat(userRepository.countByClubId(null)).isEqualTo(2);
    }

    @Test
    @DisplayName("구단별·응원 스타일별 회원 수를 집계하고 스타일이 없는 회원은 스타일 집계에서 뺀다")
    void countsGrouped() {
        // given
        persist("p1", 1L);
        persist("p2", 1L);
        User noStyle = persist("p3", 2L);
        ReflectionTestUtils.setField(noStyle, "watchStyle", null);
        flushAndClear();

        // when
        Map<Long, Long> countByClubId = userRepository.countGroupedByClubId();
        Map<String, Long> countByWatchStyle = userRepository.countGroupedByWatchStyle();

        // then
        assertThat(countByClubId).containsOnly(Map.entry(1L, 2L), Map.entry(2L, 1L));
        assertThat(countByWatchStyle).containsOnly(Map.entry("응원형", 2L));
    }

    @Test
    @DisplayName("이벤트 알림을 켠 회원만 조회한다")
    void findsEventAlarmEnabled() {
        // given
        User on = persist("p1", 1L);
        User off = persist("p2", 1L);
        off.updateAlarm(UserAlarmType.EVENT, false);
        flushAndClear();

        // when & then
        assertThat(userRepository.findAllEventAlarmEnabled())
                .extracting(User::getId)
                .containsExactly(on.getId());
    }

    @Test
    @DisplayName("탈퇴(소프트 삭제)한 회원은 조회·집계에서 빠진다")
    void excludesSoftDeleted() {
        // given
        User active = persist("p1", 1L);
        User deleted = UserFixture.newUser("p2", 1L);
        ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        entityManager.persist(deleted);
        flushAndClear();

        // when & then
        assertThat(userRepository.findAllByIds(List.of(active.getId(), deleted.getId())))
                .extracting(User::getId)
                .containsExactly(active.getId());
        assertThat(userRepository.countByClubId(1L)).isEqualTo(1);
    }
}
