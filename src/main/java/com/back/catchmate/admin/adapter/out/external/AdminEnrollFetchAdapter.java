package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminEnrollInfo;
import com.back.catchmate.admin.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.enroll.application.dto.response.EnrollInternalResponse;
import com.back.catchmate.enroll.application.port.in.EnrollInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollInternalQueryUseCase enrollInternalQueryUseCase;

    @Override
    public List<AdminEnrollInfo> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollInternalQueryUseCase.getEnrollListByBoardIds(boardIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private AdminEnrollInfo fromInternalResponse(EnrollInternalResponse response) {
        return new AdminEnrollInfo(
                response.enrollId(),
                response.userId(),
                response.acceptStatus(),
                response.requestedAt()
        );
    }
}
