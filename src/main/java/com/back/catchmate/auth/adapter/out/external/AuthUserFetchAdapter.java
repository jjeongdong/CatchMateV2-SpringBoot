package com.back.catchmate.auth.adapter.out.external;

import com.back.catchmate.auth.application.port.out.external.UserFetchPort;
import com.back.catchmate.auth.application.port.out.dto.AuthUserInfo;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public AuthUserInfo getUser(Long userId) {
        UserInternalResponse response = userInternalQueryUseCase.getUser(userId);
        return new AuthUserInfo(response.userId(), response.authority());
    }
}
