package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.board.application.BoardCommandService;
import com.back.catchmate.board.application.event.BoardEnrollAcceptedListener;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.exception.BoardFullException;
import com.back.catchmate.board.infrastructure.BoardJpaRepository;
import com.back.catchmate.board.infrastructure.BoardRepositoryImpl;
import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.infrastructure.EnrollJpaRepository;
import com.back.catchmate.enroll.infrastructure.EnrollRepositoryImpl;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.global.config.web.RetryConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 수락 → (BEFORE_COMMIT 리스너) → 인원 증가가 한 트랜잭션이라 정원 초과가 수락까지 롤백하는지 실제 DB 로 확인한다.
// 테스트가 트랜잭션을 열면 커밋이 일어나지 않아 BEFORE_COMMIT 이 돌지 않으므로 테스트 트랜잭션을 끈다.
// 운영 RDS 를 가리키는 dev 프로필을 끄고 컨테이너 DB 에만 붙는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    QuerydslConfig.class,
    JpaAuditingConfig.class,
    RetryConfig.class,
    EnrollAcceptExecutor.class,
    EnrollRepositoryImpl.class,
    BoardCommandService.class,
    BoardRepositoryImpl.class,
    BoardEnrollAcceptedListener.class
})
@Testcontainers(disabledWithoutDocker = true)
class EnrollAcceptCapacityIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockitoBean
    private GameQueryApi gameQueryApi;

    @Autowired
    private EnrollAcceptExecutor enrollAcceptExecutor;

    @Autowired
    private BoardJpaRepository boardJpaRepository;

    @Autowired
    private EnrollJpaRepository enrollJpaRepository;

    @Test
    @DisplayName("정원이 찬 게시글의 신청을 수락하면 BoardFullException 이고 신청은 대기 상태로 남는다")
    void acceptBeyondCapacityRollsBack() {
        // given (정원 2 — 작성자 1명 + 1자리)
        Board board = boardJpaRepository.save(
                Board.create(9L, "t", "c", 2, null, null, false, null, null, false, LocalDateTime.now()));
        Enroll first = enrollJpaRepository.save(Enroll.create(1L, board.getId(), 9L, "a"));
        Enroll second = enrollJpaRepository.save(Enroll.create(2L, board.getId(), 9L, "b"));
        enrollAcceptExecutor.accept(9L, first.getId());

        // when & then
        assertThatThrownBy(() -> enrollAcceptExecutor.accept(9L, second.getId()))
                .isInstanceOf(BoardFullException.class);
        assertThat(enrollJpaRepository.findById(second.getId()).orElseThrow().getAcceptStatus())
                .isEqualTo(AcceptStatus.PENDING);
        assertThat(enrollJpaRepository.findById(first.getId()).orElseThrow().getAcceptStatus())
                .isEqualTo(AcceptStatus.ACCEPTED);
        assertThat(boardJpaRepository.findById(board.getId()).orElseThrow().getCurrentPerson())
                .isEqualTo(2);
    }
}
