package com.back.catchmate.user.application.port.out;

import com.back.catchmate.enroll.application.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.domain.model.Enroll;
import java.util.List;

public interface EnrollFetchPort {
    EnrollCancelResponse deleteEnroll(Long userId, Long enrollId);
    void deleteEnroll(Enroll enroll);
    List<Enroll> getAcceptedEnrollsBetween(Long applicantId, Long boardOwnerId);
}
