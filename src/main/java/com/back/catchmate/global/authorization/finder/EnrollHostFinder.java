package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.global.authorization.common.DomainFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollHostFinder implements DomainFinder<Board> {
    private final EnrollService enrollService;
    private final BoardService boardService;

    @Override
    public Board searchById(Long enrollId) {
        Enroll enroll = enrollService.getEnroll(enrollId);
        return boardService.getBoard(enroll.getBoardId());
    }
}
