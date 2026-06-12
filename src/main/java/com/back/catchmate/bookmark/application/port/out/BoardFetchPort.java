package com.back.catchmate.bookmark.application.port.out;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.domain.model.Board;

import java.util.List;

public interface BoardFetchPort {
    BoardDetailResponse getBoard(Long userId, Long boardId);
    Board getBoard(Long boardId);
    List<Board> getBoards(List<Long> boardIds);
}
