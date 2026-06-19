package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.enroll.application.port.in.EnrollInternalQueryUseCase;
import com.back.catchmate.notification.application.port.out.external.EnrollFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollInternalQueryUseCase enrollInternalQueryUseCase;

    @Override
    public Optional<String> findAcceptStatusById(Long id) {
        return enrollInternalQueryUseCase.findAcceptStatusById(id);
    }

    @Override
    public Map<Long, String> getAcceptStatusMapByIds(List<Long> ids) {
        return enrollInternalQueryUseCase.getAcceptStatusMapByIds(ids);
    }
}
