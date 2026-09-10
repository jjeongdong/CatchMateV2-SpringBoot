package com.back.catchmate.board.repository;

import com.back.catchmate.board.dto.request.BoardSearchCondition;
import com.back.catchmate.board.entity.Board;
import com.back.catchmate.common.response.CursorPage;

public interface BoardRepositoryCustom {
    CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size);
}
