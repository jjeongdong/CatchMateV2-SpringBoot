package com.back.catchmate.board.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardSearchCondition;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import java.time.LocalDateTime;
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
@Import({QuerydslConfig.class, JpaAuditingConfig.class, BoardRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class BoardRepositoryImplQueryTest {

    // DB 정밀도(마이크로초)와 어긋나지 않게 고정 시각을 쓴다.
    private static final LocalDateTime T = LocalDateTime.of(2026, 9, 1, 12, 0);

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private BoardRepositoryImpl boardRepository;

    private Board publish(Long writerId, int maxPerson, Long clubId, Long gameId, LocalDateTime liftUpDate) {
        return boardRepository.save(
                Board.create(writerId, "t", "c", maxPerson, clubId, gameId, true, null, null, true, liftUpDate));
    }

    private static BoardSearchCondition all(LocalDateTime lastLiftUpDate, Long lastBoardId) {
        return new BoardSearchCondition(null, null, null, null, null, lastLiftUpDate, lastBoardId);
    }

    @Test
    @DisplayName("끌어올린 시각이 같으면 id 로 순서를 정해 페이지 경계에서 누락·중복이 없다")
    void cursorBreaksTiesById() {
        // given
        Board first = publish(1L, 4, 1L, 100L, T);
        Board second = publish(1L, 4, 1L, 100L, T);
        Board third = publish(1L, 4, 1L, 100L, T);

        // when
        List<Board> page1 = boardRepository.findAllByCondition(all(null, null), 2);
        Board last = page1.get(1);
        List<Board> page2 = boardRepository.findAllByCondition(all(last.getLiftUpDate(), last.getId()), 2);

        // then
        assertThat(page1).extracting(Board::getId).containsExactly(third.getId(), second.getId());
        assertThat(page2).extracting(Board::getId).containsExactly(first.getId());
    }

    @Test
    @DisplayName("임시저장·삭제 글과 차단한 작성자의 글은 빠지고, 조건 필터가 적용된다")
    void filters() {
        // given
        Board matched = publish(1L, 4, 1L, 100L, T.plusMinutes(1));
        publish(2L, 4, 1L, 100L, T.plusMinutes(2)); // 차단한 작성자
        publish(1L, 3, 1L, 100L, T.plusMinutes(3)); // 인원 불일치
        publish(1L, 4, 2L, 100L, T.plusMinutes(4)); // 구단 불일치
        publish(1L, 4, 1L, 200L, T.plusMinutes(5)); // 경기 불일치
        boardRepository.save(Board.create(1L, "임시", "", 4, 1L, 100L, false, null, null, false, T.plusMinutes(6)));
        Board deleted = publish(1L, 4, 1L, 100L, T.plusMinutes(7));
        deleted.delete(1L, T);
        boardRepository.save(deleted);

        // when
        List<Board> result = boardRepository.findAllByCondition(
                new BoardSearchCondition(List.of(100L), 4, List.of(1L), List.of(2L), null, null, null), 10);

        // then
        assertThat(result).extracting(Board::getId).containsExactly(matched.getId());
    }

    @Test
    @DisplayName("작성자 조건을 주면 그 작성자의 발행 글만 끌어올린 순으로 나온다")
    void filtersByWriter() {
        // given
        Board older = publish(1L, 4, 1L, 100L, T);
        Board newer = publish(1L, 4, 1L, 100L, T.plusDays(1));
        publish(2L, 4, 1L, 100L, T.plusDays(2));

        // when
        List<Board> result = boardRepository.findAllByCondition(
                new BoardSearchCondition(null, null, null, null, 1L, null, null), 10);

        // then
        assertThat(result).extracting(Board::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    @DisplayName("관리자 목록: 전체는 발행 글만, 유저별은 임시저장 포함으로 세고 페이징한다")
    void adminListsAndCounts() {
        // given
        Board a = publish(1L, 4, 1L, 100L, T);
        Board b = publish(2L, 4, 1L, 100L, T);
        Board draft = boardRepository.save(
                Board.create(1L, "임시", "", 4, null, null, false, null, null, false, T.plusDays(1)));

        // when & then
        assertThat(boardRepository.findAllPublished(0, 10))
                .extracting(Board::getId)
                .containsExactly(b.getId(), a.getId());
        assertThat(boardRepository.findAllPublished(1, 1))
                .extracting(Board::getId)
                .containsExactly(a.getId());
        assertThat(boardRepository.countPublished()).isEqualTo(2);
        assertThat(boardRepository.findAllByWriterId(1L, 0, 10))
                .extracting(Board::getId)
                .containsExactly(draft.getId(), a.getId());
        assertThat(boardRepository.countByWriterId(1L)).isEqualTo(2);
        assertThat(boardRepository.findDraftByWriterId(1L)).isPresent();
        assertThat(boardRepository.count()).isEqualTo(3);
    }
}
