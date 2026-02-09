package com.back.catchmate.authorization.finder;

import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.domain.common.permission.DomainFinder;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.enroll.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollHostFinder implements DomainFinder<ResourceOwnership> {
    private final EnrollService enrollService;

    @Override
    public ResourceOwnership searchById(Long enrollId) {
        Enroll enroll = enrollService.getEnroll(enrollId);
        return enroll.getBoard();
    }
}
