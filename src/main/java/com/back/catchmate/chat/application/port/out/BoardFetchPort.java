package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;

import java.util.List;
import java.util.function.Predicate;

public interface BoardFetchPort {
    Board getBoard(Long boardId);
    List<Board> getBoards(List<Long> boardIds);
    List<BoardResponse> buildBoardResponses(List<Board> boards, Predicate<Long> bookmarkedPredicate);
}
