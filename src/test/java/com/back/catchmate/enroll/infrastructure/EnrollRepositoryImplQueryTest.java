package com.back.catchmate.enroll.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
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

// 운영 RDS 를 가리키는 dev 프로필을 끄고 컨테이너 DB 에만 붙는다 (BoardRepositoryImplQueryTest 와 같은 이유).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, EnrollRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class EnrollRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private EnrollRepositoryImpl enrollRepository;

    @Test
    @DisplayName("받은 신청은 가장 최근 신청이 들어온 게시글부터 묶어 페이징하고, 대기 신청만 센다")
    void receivedBoardsOrderedByLatestPending() {
        // given (작성자 9 의 게시글 10, 11. 11 에 나중에 신청이 들어온다)
        enrollRepository.save(Enroll.create(1L, 10L, 9L, "a"));
        Enroll accepted = Enroll.create(2L, 10L, 9L, "b");
        accepted.accept(9L);
        enrollRepository.save(accepted);
        enrollRepository.save(Enroll.create(3L, 11L, 9L, "c"));

        // when & then (시각이 같아도 boardId 내림차순 보조 정렬로 11 이 먼저)
        assertThat(enrollRepository.findBoardIdsWithPendingByOwnerId(9L, 0, 10)).containsExactly(11L, 10L);
        assertThat(enrollRepository.findBoardIdsWithPendingByOwnerId(9L, 1, 1)).containsExactly(10L);
        assertThat(enrollRepository.countBoardsWithPendingByOwnerId(9L)).isEqualTo(2);
        assertThat(enrollRepository.countPendingByOwnerId(9L)).isEqualTo(2);
        assertThat(enrollRepository.findPendingByBoardIds(List.of(10L, 11L))).hasSize(2);
        assertThat(enrollRepository.findPendingByBoardId(10L, 0, 10))
                .extracting(Enroll::getUserId)
                .containsExactly(1L);
        assertThat(enrollRepository.countPendingByBoardId(10L)).isEqualTo(1);
    }

    @Test
    @DisplayName("보낸 신청은 최신순으로 페이징하고, 상태·수락 목록을 조회한다")
    void sentEnrolls() {
        // given
        Enroll first = enrollRepository.save(Enroll.create(1L, 10L, 9L, "a"));
        Enroll second = Enroll.create(1L, 11L, 9L, "b");
        second.accept(9L);
        enrollRepository.save(second);

        // when & then
        assertThat(enrollRepository.findAllByApplicantId(1L, 0, 10))
                .extracting(Enroll::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(enrollRepository.countByApplicantId(1L)).isEqualTo(2);
        assertThat(enrollRepository.findByApplicantIdAndBoardId(1L, 10L)).isPresent();
        assertThat(enrollRepository.findAcceptedByApplicantIdAndOwnerId(1L, 9L))
                .extracting(Enroll::getId)
                .containsExactly(second.getId());
        assertThat(enrollRepository.findAcceptStatusesByIds(List.of(first.getId(), second.getId())))
                .containsEntry(first.getId(), AcceptStatus.PENDING)
                .containsEntry(second.getId(), AcceptStatus.ACCEPTED);
        assertThat(enrollRepository.findAcceptStatusById(second.getId())).contains(AcceptStatus.ACCEPTED);
    }
}
