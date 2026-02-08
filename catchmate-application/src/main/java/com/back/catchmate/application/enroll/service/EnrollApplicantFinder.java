package com.back.catchmate.application.enroll.service;

import com.back.catchmate.domain.common.permission.DomainFinder;
import com.back.catchmate.domain.enroll.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollApplicantFinder implements DomainFinder<Enroll> {
    private final EnrollService enrollService;

    @Override
    public Enroll searchById(Long enrollId) {
        return enrollService.getEnroll(enrollId);
    }
}
