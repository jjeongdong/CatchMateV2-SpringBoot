package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.notification.application.port.out.EnrollFetchPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollService enrollService;

    @Override
    public Optional<AcceptStatus> findAcceptStatusById(Long id) {
        return enrollService.findAcceptStatusById(id);
    }

    @Override
    public Map<Long, AcceptStatus> getAcceptStatusMapByIds(List<Long> ids) {
        return enrollService.getAcceptStatusMapByIds(ids);
    }
}
