package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardEnrollInfo;
import com.back.catchmate.board.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.enroll.application.port.in.EnrollInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BoardEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollInternalQueryUseCase enrollInternalQueryUseCase;

    @Override
    public Optional<BoardEnrollInfo> findEnrollByUserIdAndBoardId(Long userId, Long boardId) {
        return enrollInternalQueryUseCase.findEnrollByUserIdAndBoardId(userId, boardId)
                .map(response -> new BoardEnrollInfo(response.enrollId(), response.acceptStatus()));
    }
}
