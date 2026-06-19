package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;
import com.back.catchmate.enroll.application.port.out.external.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollBoardFetchAdapter implements BoardFetchPort {
    private final BoardInternalQueryUseCase boardInternalQueryUseCase;

    @Override
    public EnrollBoardInfo getBoard(Long boardId) {
        return toEnrollBoardInfo(boardInternalQueryUseCase.getBoard(boardId));
    }

    @Override
    public EnrollBoardInfo getBoardWithLock(Long boardId) {
        return toEnrollBoardInfo(boardInternalQueryUseCase.getBoardWithLock(boardId));
    }

    @Override
    public EnrollBoardInfo getCompletedBoard(Long boardId) {
        return toEnrollBoardInfo(boardInternalQueryUseCase.getCompletedBoard(boardId));
    }

    @Override
    public List<EnrollBoardInfo> getBoards(List<Long> boardIds) {
        return boardInternalQueryUseCase.getBoards(boardIds).stream()
                .map(this::toEnrollBoardInfo)
                .toList();
    }

    private EnrollBoardInfo toEnrollBoardInfo(BoardInternalResponse response) {
        return new EnrollBoardInfo(
                response.boardId(),
                response.userId(),
                response.title(),
                response.content(),
                response.currentPerson(),
                response.maxPerson() != null ? response.maxPerson() : 0,
                response.cheerClubId(),
                response.gameId()
        );
    }
}
