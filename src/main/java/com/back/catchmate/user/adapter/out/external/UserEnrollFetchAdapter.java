package com.back.catchmate.user.adapter.out.external;

import com.back.catchmate.enroll.application.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.application.port.out.EnrollFetchPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEnrollFetchAdapter implements EnrollFetchPort {
    private final EnrollService enrollService;

    @Override
    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        return enrollService.deleteEnroll(userId, enrollId);
    }

    @Override
    public void deleteEnroll(Enroll enroll) {
        enrollService.deleteEnroll(enroll);
    }

    @Override
    public List<Enroll> getAcceptedEnrollsBetween(Long applicantId, Long boardOwnerId) {
        return enrollService.getAcceptedEnrollsBetween(applicantId, boardOwnerId);
    }
}
