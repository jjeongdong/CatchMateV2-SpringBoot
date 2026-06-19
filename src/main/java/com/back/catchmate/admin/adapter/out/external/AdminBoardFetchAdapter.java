package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminBoardInfo;
import com.back.catchmate.admin.application.port.out.external.BoardFetchPort;
import com.back.catchmate.board.application.dto.response.BoardAdminView;
import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardAdminQueryUseCase;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBoardFetchAdapter implements BoardFetchPort {
    private final BoardAdminQueryUseCase boardAdminQueryUseCase;
    private final BoardInternalQueryUseCase boardInternalQueryUseCase;

    @Override
    public Page<AdminBoardInfo> getBoardList(Pageable pageable) {
        return boardAdminQueryUseCase.getBoardList(pageable).map(this::fromAdminView);
    }

    @Override
    public Page<AdminBoardInfo> getBoardListByUserId(Long userId, Pageable pageable) {
        return boardAdminQueryUseCase.getBoardListByUserId(userId, pageable).map(this::fromAdminView);
    }

    @Override
    public AdminBoardInfo getCompletedBoard(Long boardId) {
        return fromInternalResponse(boardInternalQueryUseCase.getCompletedBoard(boardId));
    }

    @Override
    public long getTotalBoardCount() {
        return boardAdminQueryUseCase.getTotalBoardCount();
    }

    private AdminBoardInfo fromInternalResponse(BoardInternalResponse response) {
        return new AdminBoardInfo(
                response.boardId(),
                response.title(),
                response.content(),
                response.maxPerson() != null ? response.maxPerson() : 0,
                response.currentPerson(),
                response.userId(),
                response.gameId(),
                response.completed(),
                response.createdAt()
        );
    }

    private AdminBoardInfo fromAdminView(BoardAdminView view) {
        return new AdminBoardInfo(
                view.boardId(),
                view.title(),
                view.content(),
                view.maxPerson(),
                view.currentPerson(),
                view.userId(),
                view.gameId(),
                view.completed(),
                view.createdAt()
        );
    }
}
