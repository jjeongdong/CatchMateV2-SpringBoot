package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.global.authorization.common.DomainFinder;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.enroll.domain.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollHostFinder implements DomainFinder<Board> {
    private final EnrollService enrollService;

    @Override
    public Board searchById(Long enrollId) {
        Enroll enroll = enrollService.getEnroll(enrollId);
        return enroll.getBoard();
    }
}
