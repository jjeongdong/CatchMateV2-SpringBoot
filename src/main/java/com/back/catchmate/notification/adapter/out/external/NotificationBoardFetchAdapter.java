package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.notification.application.port.out.dto.NotificationBoardInfo;
import com.back.catchmate.notification.application.port.out.external.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationBoardFetchAdapter implements BoardFetchPort {
    private final BoardService boardService;

    @Override
    public NotificationBoardInfo getBoard(Long boardId) {
        return fromInternalResponse(boardService.getBoardSummary(boardId));
    }

    @Override
    public List<NotificationBoardInfo> getBoards(List<Long> boardIds) {
        return boardService.getBoardSummaries(boardIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationBoardInfo fromInternalResponse(BoardSummary response) {
        if (response == null) return null;
        return new NotificationBoardInfo(
                response.boardId(),
                response.gameId(),
                response.title()
        );
    }
}
