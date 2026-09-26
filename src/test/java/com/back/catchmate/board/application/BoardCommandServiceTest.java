package com.back.catchmate.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.result.BoardCreateResult;
import com.back.catchmate.board.application.dto.result.BoardLiftUpResult;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.event.BoardCompletedEvent;
import com.back.catchmate.board.fixture.BoardFixture;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BoardCommandService boardCommandService;

    private static GameInfo completeGame() {
        return new GameInfo(100L, LocalDateTime.of(2026, 10, 1, 18, 30), "잠실", 1L, 2L);
    }

    private void saveAssignsId(Long boardId) {
        willAnswer(invocation -> {
                    Board board = invocation.getArgument(0);
                    ReflectionTestUtils.setField(board, "id", boardId);
                    return board;
                })
                .given(boardRepository)
                .save(any(Board.class));
    }

    @Test
    @DisplayName("발행 글을 만들면 기존 임시저장 글을 지우고 발행 이벤트를 낸다")
    void createPublishedBoard() {
        // given
        Board draft = BoardFixture.draft(9L, 1L);
        given(boardRepository.findDraftByWriterId(1L)).willReturn(Optional.of(draft));
        given(gameQueryApi.getInfo(100L)).willReturn(completeGame());
        saveAssignsId(10L);
        BoardCreateCommand command = new BoardCreateCommand("t", "c", 4, 1L, null, List.of("20대"), 100L, true);

        // when
        BoardCreateResult result = boardCommandService.createBoard(1L, command);

        // then
        assertThat(result.boardId()).isEqualTo(10L);
        then(boardRepository).should().deleteDraft(draft);
        then(eventPublisher).should().publishEvent(new BoardCompletedEvent(10L, 1L));
    }

    @Test
    @DisplayName("임시저장은 경기를 조회하지 않고 이벤트도 내지 않는다")
    void createDraft() {
        // given
        given(boardRepository.findDraftByWriterId(1L)).willReturn(Optional.empty());
        saveAssignsId(10L);

        // when
        boardCommandService.createBoard(1L, new BoardCreateCommand("t", "", 4, null, null, null, null, false));

        // then
        then(gameQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("임시저장 글을 발행으로 바꿀 때만 발행 이벤트를 낸다")
    void updatePublishesOnlyOnFirstPublish() {
        // given
        Board draft = BoardFixture.draft(10L, 1L);
        given(boardRepository.getById(10L)).willReturn(draft);
        given(gameQueryApi.getInfo(100L)).willReturn(completeGame());
        BoardUpdateCommand publish = new BoardUpdateCommand("t", "c", 4, 1L, null, null, 100L, true);

        // when
        boardCommandService.updateBoard(1L, 10L, publish);
        boardCommandService.updateBoard(1L, 10L, publish);

        // then
        then(eventPublisher).should().publishEvent(new BoardCompletedEvent(10L, 1L));
    }

    @Test
    @DisplayName("3일이 지나지 않았으면 끌어올리지 않고 남은 시간을 돌려준다")
    void liftUpTooEarly() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        ReflectionTestUtils.setField(board, "liftUpDate", LocalDateTime.now().minusDays(1));
        given(boardRepository.getById(10L)).willReturn(board);

        // when
        BoardLiftUpResult result = boardCommandService.liftUpBoard(1L, 10L);

        // then
        assertThat(result.liftedUp()).isFalse();
        // 다음 가능 시각까지 2일에서 테스트 실행 시간(초 단위)만 모자라고, 분은 올림한다.
        assertThat(result.remainingTime()).isEqualTo("2일 00시간 00분");
    }

    @Test
    @DisplayName("3일이 지났으면 끌어올리고 남은 시간은 없다")
    void liftUp() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        ReflectionTestUtils.setField(board, "liftUpDate", LocalDateTime.now().minusDays(4));
        given(boardRepository.getById(10L)).willReturn(board);

        // when
        BoardLiftUpResult result = boardCommandService.liftUpBoard(1L, 10L);

        // then
        assertThat(result.liftedUp()).isTrue();
        assertThat(result.remainingTime()).isNull();
    }

    @Test
    @DisplayName("삭제하면 삭제 시각이 남는다")
    void deleteBoard() {
        // given
        Board board = BoardFixture.published(10L, 1L);
        given(boardRepository.getById(10L)).willReturn(board);

        // when
        boardCommandService.deleteBoard(1L, 10L);

        // then
        assertThat(board.getDeletedAt()).isNotNull();
    }
}
