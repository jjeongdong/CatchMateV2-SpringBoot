package com.back.catchmate.authorization.finder;

import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.authorization.common.DomainFinder;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.enroll.model.Enroll;
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
