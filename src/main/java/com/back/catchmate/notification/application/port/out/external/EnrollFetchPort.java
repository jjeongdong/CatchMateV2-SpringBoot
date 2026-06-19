package com.back.catchmate.notification.application.port.out.external;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollFetchPort {
    Optional<String> findAcceptStatusById(Long id);

    Map<Long, String> getAcceptStatusMapByIds(List<Long> ids);
}
