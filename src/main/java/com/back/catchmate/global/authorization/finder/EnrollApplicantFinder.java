package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.global.authorization.common.DomainFinder;
import com.back.catchmate.enroll.domain.model.Enroll;
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
