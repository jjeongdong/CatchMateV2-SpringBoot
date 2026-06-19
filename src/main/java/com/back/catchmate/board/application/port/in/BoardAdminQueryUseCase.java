package com.back.catchmate.board.application.port.in;

import com.back.catchmate.board.application.dto.response.BoardAdminView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardAdminQueryUseCase {
    Page<BoardAdminView> getBoardList(Pageable pageable);

    Page<BoardAdminView> getBoardListByUserId(Long userId, Pageable pageable);

    long getTotalBoardCount();
}
