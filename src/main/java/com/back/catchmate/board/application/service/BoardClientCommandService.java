package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.application.event.BoardCompletedEvent;
import com.back.catchmate.board.application.port.in.BoardClientCommandUseCase;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.external.GameFetchPort;
import com.back.catchmate.board.application.port.out.persistence.BoardRepository;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.PreferredAgeRange;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardClientCommandService implements BoardClientCommandUseCase {
    private final BoardRepository boardRepository;
    private final BoardReader boardReader;
    private final GameFetchPort gameFetchPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        boardReader.findTempBoard(userId).ifPresent(boardRepository::deleteTempBoard);

        BoardGameInfo game = resolveGame(command.gameId());

        Board board = Board.createBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                userId,
                command.cheerClubId(),
                game != null ? game.gameId() : null,
                game != null && game.isComplete(),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed()
        );

        Board savedBoard = boardRepository.save(board);

        if (command.completed()) {
            applicationEventPublisher.publishEvent(BoardCompletedEvent.of(savedBoard.getId(), userId));
        }

        return BoardCreateResponse.from(savedBoard);
    }

    @Override
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = boardReader.getBoard(boardId);
        verifyBoardOwner(board, userId);
        boolean wasCompleted = board.isCompleted();
        BoardGameInfo game = resolveGame(command.gameId());

        board.updateBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                command.cheerClubId(),
                game != null ? game.gameId() : null,
                game != null && game.isComplete(),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed()
        );

        boardRepository.save(board);

        if (!wasCompleted && command.completed()) {
            applicationEventPublisher.publishEvent(BoardCompletedEvent.of(board.getId(), userId));
        }

        return BoardUpdateResponse.from(board);
    }

    @Override
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = boardReader.getBoard(boardId);
        verifyBoardOwner(board, userId);

        if (!board.canLiftUp()) {
            long remainingMinutes = board.getRemainingMinutesForLiftUp();
            return BoardLiftUpResponse.fromRemainingMinutes(false, remainingMinutes);
        }

        board.updateLiftUpDate(LocalDateTime.now());
        boardRepository.save(board);
        return BoardLiftUpResponse.of(true, null);
    }

    @Override
    public void deleteBoard(Long userId, Long boardId) {
        Board board = boardReader.getBoard(boardId);
        verifyBoardOwner(board, userId);
        board.delete();                 // 완성 게시글: soft delete (deletedAt 세팅)
        boardRepository.save(board);
    }

    private void verifyBoardOwner(Board board, Long userId) {
        if (!board.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private BoardGameInfo resolveGame(Long gameId) {
        if (gameId == null) {
            return null;
        }
        return gameFetchPort.getGame(gameId);
    }
}
