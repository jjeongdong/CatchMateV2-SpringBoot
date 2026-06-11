package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.EnrollFetchPort;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.Enroll;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollService enrollService;

    @Override
    public List<Enroll> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollService.getEnrollListByBoardIds(boardIds);
    }
}
