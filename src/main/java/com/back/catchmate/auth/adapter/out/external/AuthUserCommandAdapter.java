package com.back.catchmate.auth.adapter.out.external;

import com.back.catchmate.auth.application.port.out.external.UserCommandPort;
import com.back.catchmate.user.application.port.in.UserInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserCommandAdapter implements UserCommandPort {
    private final UserInternalCommandUseCase userInternalCommandUseCase;

    @Override
    public void clearFcmToken(Long userId) {
        userInternalCommandUseCase.clearFcmToken(userId);
    }
}
