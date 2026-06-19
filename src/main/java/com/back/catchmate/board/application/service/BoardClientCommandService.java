package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.command.GameCreateCommand;
import com.back.catchmate.board.application.dto.command.GameUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.application.event.BoardCompletedEvent;
import com.back.catchmate.board.application.port.in.BoardClientCommandUseCase;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.dto.BoardGameUpsertCommand;
import com.back.catchmate.board.application.port.out.external.GameCommandPort;
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
    private final GameCommandPort gameCommandPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        boardReader.findTempBoard(userId).ifPresent(boardRepository::delete);

        BoardGameInfo game = resolveGame(command.gameCreateCommand());

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
        BoardGameInfo game = resolveGame(command.gameUpdateCommand());

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
        board.delete();
        boardRepository.save(board);
    }

    private void verifyBoardOwner(Board board, Long userId) {
        if (!board.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private BoardGameInfo resolveGame(GameCreateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
    }

    private BoardGameInfo resolveGame(GameUpdateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
    }

    private BoardGameInfo resolveGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        if (gameStartDate == null && homeClubId == null && awayClubId == null && location == null) {
            return null;
        }
        BoardGameUpsertCommand upsertCommand = new BoardGameUpsertCommand(homeClubId, awayClubId, gameStartDate, location);
        if (homeClubId != null && awayClubId != null && gameStartDate != null) {
            return gameCommandPort.findOrCreateGame(upsertCommand);
        }
        return gameCommandPort.savePartialGame(upsertCommand);
    }
}
