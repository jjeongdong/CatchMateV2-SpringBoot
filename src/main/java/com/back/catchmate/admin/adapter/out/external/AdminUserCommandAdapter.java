package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.UserCommandPort;
import com.back.catchmate.user.application.port.in.UserInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserCommandAdapter implements UserCommandPort {
    private final UserInternalCommandUseCase userInternalCommandUseCase;

    @Override
    public void markUserAsReported(Long userId) {
        userInternalCommandUseCase.markUserAsReported(userId);
    }
}
