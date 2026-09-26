package com.back.catchmate.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.board.domain.exception.BoardCheerClubMissingException;
import com.back.catchmate.board.domain.exception.BoardContentMissingException;
import com.back.catchmate.board.domain.exception.BoardFullException;
import com.back.catchmate.board.domain.exception.BoardGameMissingException;
import com.back.catchmate.board.domain.exception.BoardNotEditableAfterEnrollException;
import com.back.catchmate.board.domain.exception.BoardNotWriterException;
import com.back.catchmate.board.domain.exception.BoardTitleMissingException;
import com.back.catchmate.board.fixture.BoardFixture;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardTest {

    private static final LocalDateTime NOW = BoardFixture.NOW;

    @Test
    @DisplayName("임시저장 글은 발행 검증 없이 만들어지고 작성자 1명으로 시작한다")
    void createDraftWithoutValidation() {
        // when
        Board board = Board.create(1L, "", "", 4, null, null, false, null, null, false, NOW);

        // then
        assertThat(board.getCurrentPerson()).isEqualTo(1);
        assertThat(board.getLiftUpDate()).isEqualTo(NOW);
        assertThat(board.getPreferredAgeRange()).isEqualTo(PreferredAgeRange.empty());
        assertThat(board.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("발행 글은 제목·내용·응원 구단·완전한 경기 정보가 모두 있어야 한다")
    void publishRequiresAllFields() {
        assertThatThrownBy(() -> Board.create(1L, " ", "c", 4, 1L, 100L, true, null, null, true, NOW))
                .isInstanceOf(BoardTitleMissingException.class);
        assertThatThrownBy(() -> Board.create(1L, "t", null, 4, 1L, 100L, true, null, null, true, NOW))
                .isInstanceOf(BoardContentMissingException.class);
        assertThatThrownBy(() -> Board.create(1L, "t", "c", 4, null, 100L, true, null, null, true, NOW))
                .isInstanceOf(BoardCheerClubMissingException.class);
        assertThatThrownBy(() -> Board.create(1L, "t", "c", 4, 1L, null, true, null, null, true, NOW))
                .isInstanceOf(BoardGameMissingException.class);
        assertThatThrownBy(() -> Board.create(1L, "t", "c", 4, 1L, 100L, false, null, null, true, NOW))
                .isInstanceOf(BoardGameMissingException.class);
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없다")
    void editRejectsOtherUser() {
        // given
        Board board = BoardFixture.published(10L, 1L);

        // when & then
        assertThatThrownBy(() -> board.edit(2L, "t", "c", 4, 1L, 100L, true, "F", null, true))
                .isInstanceOf(BoardNotWriterException.class);
    }

    @Test
    @DisplayName("수락된 신청자가 있으면 핵심 조건(인원)은 바꿀 수 없다")
    void editRejectsCriticalChangeAfterEnroll() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        board.increaseCurrentPerson();

        // when & then
        assertThatThrownBy(() -> board.edit(
                        1L, "같이 직관가요", "3루 응원석", 5, 1L, 100L, true, "F", PreferredAgeRange.of(List.of("20대")), true))
                .isInstanceOf(BoardNotEditableAfterEnrollException.class);
    }

    @Test
    @DisplayName("수락된 신청자가 있어도 제목·내용은 바꿀 수 있다")
    void editAllowsTextChangeAfterEnroll() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        board.increaseCurrentPerson();

        // when
        board.edit(1L, "새 제목", "새 내용", 4, 1L, 100L, true, "F", PreferredAgeRange.of(List.of("20대")), true);

        // then
        assertThat(board.getTitle()).isEqualTo("새 제목");
        assertThat(board.getContent()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("끌어올린 뒤 3일이 지나지 않았으면 끌어올리지 않고 false")
    void liftUpTooEarly() {
        // given
        Board board = BoardFixture.published(10L, 1L);

        // when
        boolean liftedUp = board.liftUp(1L, NOW.plusDays(3));

        // then
        assertThat(liftedUp).isFalse();
        assertThat(board.getLiftUpDate()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("3일이 지났으면 끌어올리고 true")
    void liftUpAfterInterval() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        LocalDateTime later = NOW.plusDays(3).plusMinutes(1);

        // when
        boolean liftedUp = board.liftUp(1L, later);

        // then
        assertThat(liftedUp).isTrue();
        assertThat(board.getLiftUpDate()).isEqualTo(later);
    }

    @Test
    @DisplayName("작성자가 아니면 끌어올릴 수 없다")
    void liftUpRejectsOtherUser() {
        Board board = BoardFixture.published(10L, 1L);

        assertThatThrownBy(() -> board.liftUp(2L, NOW.plusDays(4))).isInstanceOf(BoardNotWriterException.class);
    }

    @Test
    @DisplayName("남은 시간은 분 단위로 올림한다")
    void remainingMinutesRoundsUp() {
        // given
        Board board = BoardFixture.published(10L, 1L);

        // when
        long remaining = board.remainingMinutesForLiftUp(NOW.plusSeconds(90));

        // then (3일 = 4320분, 90초 경과 → 4318.5분 → 4319분)
        assertThat(remaining).isEqualTo(4319);
        assertThat(board.remainingMinutesForLiftUp(NOW.plusDays(3))).isZero();
    }

    @Test
    @DisplayName("작성자만 삭제할 수 있고, 삭제하면 삭제 시각이 남는다")
    void delete() {
        // given
        Board board = BoardFixture.published(10L, 1L);

        // when & then
        assertThatThrownBy(() -> board.delete(2L, NOW)).isInstanceOf(BoardNotWriterException.class);
        board.delete(1L, NOW);
        assertThat(board.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("정원이 차면 인원을 늘릴 수 없다")
    void increaseCurrentPersonRejectsWhenFull() {
        // given
        Board board = Board.create(1L, "t", "c", 2, 1L, 100L, true, null, null, true, NOW);
        board.increaseCurrentPerson();

        // when & then
        assertThat(board.getCurrentPerson()).isEqualTo(2);
        assertThatThrownBy(board::increaseCurrentPerson).isInstanceOf(BoardFullException.class);
    }
}
