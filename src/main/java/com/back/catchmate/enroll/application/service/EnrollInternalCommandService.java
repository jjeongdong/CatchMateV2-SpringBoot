package com.back.catchmate.enroll.application.service;

import com.back.catchmate.enroll.application.port.in.EnrollInternalCommandUseCase;
import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollInternalCommandService implements EnrollInternalCommandUseCase {
    private final EnrollRepository enrollRepository;
    private final EnrollReader enrollReader;

    @Override
    public void deleteAcceptedEnrollsBetween(Long blockerId, Long blockedId) {
        enrollReader.getAcceptedEnrollsBetween(blockerId, blockedId)
                .forEach(enrollRepository::delete);
    }
}
