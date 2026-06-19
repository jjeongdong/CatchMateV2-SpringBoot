package com.back.catchmate.enroll.application.port.in;

public interface EnrollInternalCommandUseCase {
    void deleteAcceptedEnrollsBetween(Long blockerId, Long blockedId);
}
