package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.board.domain.model.Board;

import java.util.List;

public interface BoardFetchPort {
    Board getBoard(Long boardId);
    List<Board> getBoards(List<Long> boardIds);
}
