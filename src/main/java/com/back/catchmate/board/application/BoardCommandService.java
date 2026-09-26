package com.back.catchmate.board.application;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.result.BoardCreateResult;
import com.back.catchmate.board.application.dto.result.BoardLiftUpResult;
import com.back.catchmate.board.application.dto.result.BoardUpdateResult;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.PreferredAgeRange;
import com.back.catchmate.board.domain.event.BoardCompletedEvent;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardCommandService {
    private final BoardRepository boardRepository;
    private final GameQueryApi gameQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BoardCreateResult createBoard(Long userId, BoardCreateCommand command) {
        // 임시저장 글은 사용자당 하나라 새 글을 쓰면 이전 임시저장 글을 지운다.
        boardRepository.findDraftByWriterId(userId).ifPresent(boardRepository::deleteDraft);
        Board board = boardRepository.save(Board.create(
                userId,
                command.title(),
                command.content(),
                command.maxPerson(),
                command.cheerClubId(),
                command.gameId(),
                isGameComplete(command.gameId()),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed(),
                LocalDateTime.now()));
        if (board.isCompleted()) {
            eventPublisher.publishEvent(new BoardCompletedEvent(board.getId(), userId));
        }
        return BoardCreateResult.from(board);
    }

    @Transactional
    public BoardUpdateResult updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = boardRepository.getById(boardId);
        boolean wasCompleted = board.isCompleted();
        board.edit(
                userId,
                command.title(),
                command.content(),
                command.maxPerson(),
                command.cheerClubId(),
                command.gameId(),
                isGameComplete(command.gameId()),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed());
        if (!wasCompleted && board.isCompleted()) {
            eventPublisher.publishEvent(new BoardCompletedEvent(board.getId(), userId));
        }
        return BoardUpdateResult.from(board);
    }

    @Transactional
    public BoardLiftUpResult liftUpBoard(Long userId, Long boardId) {
        Board board = boardRepository.getById(boardId);
        LocalDateTime now = LocalDateTime.now();
        if (board.liftUp(userId, now)) {
            return BoardLiftUpResult.lifted();
        }
        return BoardLiftUpResult.tooEarly(board.remainingMinutesForLiftUp(now));
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        boardRepository.getById(boardId).delete(userId, LocalDateTime.now());
    }

    /** enroll 수락 이벤트 리스너 전용. 낙관적 락(@Version) 충돌 지점이다. */
    @Transactional
    public void increaseCurrentPerson(Long boardId) {
        boardRepository.getById(boardId).increaseCurrentPerson();
    }

    // 발행에 충분한 경기 정보인지 판정한다. 경기가 없으면 게임 BC 를 부르지 않는다.
    // gameId 가 있는데 경기가 없으면 GameQueryApi 가 예외를 던진다 (옛 동작).
    private boolean isGameComplete(Long gameId) {
        if (gameId == null) {
            return false;
        }
        GameInfo game = gameQueryApi.getInfo(gameId);
        return game.homeClubId() != null
                && game.awayClubId() != null
                && game.gameStartDate() != null
                && game.location() != null;
    }
}
