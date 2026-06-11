package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.bookmark.application.port.out.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookmarkBoardFetchAdapter implements BoardFetchPort {
    private final BoardService boardService;

    @Override
    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        return boardService.getBoard(userId, boardId);
    }

    @Override
    public Board getBoard(Long boardId) {
        return boardService.getBoard(boardId);
    }
}
