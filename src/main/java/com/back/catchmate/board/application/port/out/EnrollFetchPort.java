package com.back.catchmate.board.application.port.out;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.domain.model.User;

import java.util.Optional;

public interface EnrollFetchPort {
    Optional<Enroll> findEnrollByUserAndBoard(User user, Board board);
}
