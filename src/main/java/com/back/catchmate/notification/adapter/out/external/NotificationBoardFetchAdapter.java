package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationBoardInfo;
import com.back.catchmate.notification.application.port.out.external.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationBoardFetchAdapter implements BoardFetchPort {
    private final BoardInternalQueryUseCase boardInternalQueryUseCase;

    @Override
    public NotificationBoardInfo getBoard(Long boardId) {
        return fromInternalResponse(boardInternalQueryUseCase.getBoard(boardId));
    }

    @Override
    public List<NotificationBoardInfo> getBoards(List<Long> boardIds) {
        return boardInternalQueryUseCase.getBoards(boardIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationBoardInfo fromInternalResponse(BoardInternalResponse response) {
        if (response == null) return null;
        return new NotificationBoardInfo(
                response.boardId(),
                response.gameId(),
                response.title()
        );
    }
}
