package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollFetchPort {
    Optional<AcceptStatus> findAcceptStatusById(Long id);
    Map<Long, AcceptStatus> getAcceptStatusMapByIds(List<Long> ids);
}
