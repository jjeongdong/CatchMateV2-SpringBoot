package com.back.catchmate.board.application.port.out;

import com.back.catchmate.enroll.domain.model.Enroll;

import java.util.Optional;

public interface EnrollFetchPort {
    Optional<Enroll> findEnrollByUserIdAndBoardId(Long userId, Long boardId);
}
