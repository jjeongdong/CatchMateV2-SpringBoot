package com.back.catchmate.enroll.application.port.in;

import com.back.catchmate.enroll.application.dto.response.EnrollInternalResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollInternalQueryUseCase {
    EnrollInternalResponse getEnroll(Long enrollId);

    Optional<EnrollInternalResponse> findEnrollByUserIdAndBoardId(Long userId, Long boardId);

    Optional<String> findAcceptStatusById(Long enrollId);

    Map<Long, String> getAcceptStatusMapByIds(List<Long> ids);

    List<EnrollInternalResponse> getEnrollListByBoardIds(List<Long> boardIds);
}
