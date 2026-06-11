package com.back.catchmate.bookmark.application.port.out;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.domain.model.Board;

public interface BoardFetchPort {
    BoardDetailResponse getBoard(Long userId, Long boardId);
    Board getBoard(Long boardId);
}
