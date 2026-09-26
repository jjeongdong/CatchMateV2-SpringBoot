package com.back.catchmate.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportReason;
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

// NoticeRepositoryImplQueryTest 와 같은 이유로 test 프로필 + 컨테이너 datasource 만 쓴다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, ReportRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class ReportRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private ReportRepositoryImpl reportRepository;

    @Test
    @DisplayName("최신순 페이징과 처리 여부별 건수를 센다")
    void pagesAndCounts() {
        // given
        Report first = reportRepository.save(Report.create(1L, 2L, ReportReason.SPAM, null));
        Report second = reportRepository.save(Report.create(1L, 3L, ReportReason.OTHER, null));
        second.process();
        reportRepository.save(second);

        // when
        List<Report> page = reportRepository.findAllLatest(0, 10);

        // then
        assertThat(page).extracting(Report::getId).containsExactly(second.getId(), first.getId());
        assertThat(reportRepository.count()).isEqualTo(2);
        assertThat(reportRepository.countByCompleted(false)).isEqualTo(1);
    }
}
