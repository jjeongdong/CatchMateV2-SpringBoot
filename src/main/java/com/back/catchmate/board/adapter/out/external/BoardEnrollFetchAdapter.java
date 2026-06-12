package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.EnrollFetchPort;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BoardEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollService enrollService;

    @Override
    public Optional<Enroll> findEnrollByUserIdAndBoardId(Long userId, Long boardId) {
        return enrollService.findEnrollByUserIdAndBoardId(userId, boardId);
    }
}
