package com.back.catchmate.board.application.port.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardEnrollInfo;

import java.util.Optional;

public interface EnrollFetchPort {
    Optional<BoardEnrollInfo> findEnrollByUserIdAndBoardId(Long userId, Long boardId);
}
