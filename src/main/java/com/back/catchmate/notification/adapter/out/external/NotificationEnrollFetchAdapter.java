package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.enroll.service.EnrollQueryService;
import com.back.catchmate.notification.application.port.out.external.EnrollFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollQueryService enrollQueryService;

    @Override
    public Optional<String> findAcceptStatusById(Long id) {
        return enrollQueryService.findAcceptStatusById(id);
    }

    @Override
    public Map<Long, String> getAcceptStatusMapByIds(List<Long> ids) {
        return enrollQueryService.getAcceptStatusMapByIds(ids);
    }
}
