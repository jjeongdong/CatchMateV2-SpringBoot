package com.back.catchmate.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.notice.domain.Notice;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// application.yml 의 기본 프로필 dev 는 운영 RDS 를 가리킨다. create-drop 이 운영 DB 에 닿지 않도록
// 존재하지 않는 test 프로필로 dev 설정을 끄고, datasource 는 컨테이너(@ServiceConnection)에서만 받는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, NoticeRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class NoticeRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private NoticeRepositoryImpl noticeRepository;

    @Test
    @DisplayName("최신순으로 offset/limit 을 적용하고 전체 수를 센다")
    void findsLatestWithPaging() {
        // given
        Notice first = noticeRepository.save(Notice.create(1L, "1", "c"));
        Notice second = noticeRepository.save(Notice.create(1L, "2", "c"));
        Notice third = noticeRepository.save(Notice.create(1L, "3", "c"));

        // when
        List<Notice> firstPage = noticeRepository.findAllLatest(0, 2);
        List<Notice> secondPage = noticeRepository.findAllLatest(2, 2);

        // then
        assertThat(firstPage).extracting(Notice::getId).containsExactly(third.getId(), second.getId());
        assertThat(secondPage).extracting(Notice::getId).containsExactly(first.getId());
        assertThat(noticeRepository.count()).isEqualTo(3);
    }
}
