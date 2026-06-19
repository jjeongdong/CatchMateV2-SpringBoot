package com.back.catchmate.enroll.application.port.in;

import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCreateResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRejectResponse;

public interface EnrollClientCommandUseCase {
    EnrollCreateResponse createEnroll(EnrollCreateCommand command);

    EnrollAcceptResponse updateEnrollAccept(Long userId, Long enrollId);

    EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId);

    EnrollCancelResponse deleteEnroll(Long userId, Long enrollId);

    void markEnrollAsRead(Long userId, Long enrollId);
}
