package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.dto.response.BoardAdminView;
import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardAdminQueryUseCase;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import com.back.catchmate.board.domain.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardInternalQueryService implements BoardInternalQueryUseCase, BoardAdminQueryUseCase {
    private final BoardReader boardReader;

    @Override
    public BoardInternalResponse getBoard(Long boardId) {
        return toInternalResponse(boardReader.getBoard(boardId));
    }

    @Override
    public List<BoardInternalResponse> getBoards(List<Long> boardIds) {
        return boardReader.getBoards(boardIds).stream()
                .map(this::toInternalResponse)
                .toList();
    }

    @Override
    public BoardInternalResponse getCompletedBoard(Long boardId) {
        return toInternalResponse(boardReader.getCompletedBoard(boardId));
    }

    @Override
    public Page<BoardAdminView> getBoardList(Pageable pageable) {
        return boardReader.getBoardList(pageable).map(this::toAdminView);
    }

    @Override
    public Page<BoardAdminView> getBoardListByUserId(Long userId, Pageable pageable) {
        return boardReader.getBoardListByUserId(userId, pageable).map(this::toAdminView);
    }

    @Override
    public long getTotalBoardCount() {
        return boardReader.getTotalBoardCount();
    }

    private BoardInternalResponse toInternalResponse(Board board) {
        return new BoardInternalResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.getUserId(),
                board.getCheerClubId(),
                board.getGameId(),
                board.getPreferredGender(),
                board.getPreferredAgeRange() != null ? board.getPreferredAgeRange().asList() : List.of(),
                board.isCompleted(),
                board.getCreatedAt(),
                board.getLiftUpDate()
        );
    }

    private BoardAdminView toAdminView(Board board) {
        return new BoardAdminView(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.getUserId(),
                board.getGameId(),
                board.isCompleted(),
                board.getCreatedAt()
        );
    }
}
