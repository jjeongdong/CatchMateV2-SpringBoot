package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.auth.application.service.AuthService;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.oauth.application.port.out.AuthFetchPort;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthAuthFetchAdapter implements AuthFetchPort {
    private final AuthService authService;

    @Override
    public AuthToken createToken(User user) {
        return authService.createToken(user);
    }
}
