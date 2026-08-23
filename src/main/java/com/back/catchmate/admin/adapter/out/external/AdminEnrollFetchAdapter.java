package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminEnrollInfo;
import com.back.catchmate.admin.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.enroll.dto.response.EnrollSummary;
import com.back.catchmate.enroll.service.EnrollQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollQueryService enrollQueryService;

    @Override
    public List<AdminEnrollInfo> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollQueryService.getEnrollListByBoardIds(boardIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private AdminEnrollInfo fromInternalResponse(EnrollSummary response) {
        return new AdminEnrollInfo(
                response.enrollId(),
                response.userId(),
                response.acceptStatus(),
                response.requestedAt()
        );
    }
}
