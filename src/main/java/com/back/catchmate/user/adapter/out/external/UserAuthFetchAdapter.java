package com.back.catchmate.user.adapter.out.external;

import com.back.catchmate.auth.application.service.AuthService;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.user.application.port.out.AuthFetchPort;
import com.back.catchmate.user.domain.model.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class UserAuthFetchAdapter implements AuthFetchPort {

    private final AuthService authService;

    // user signup needs an auth token, but auth depends on user — break the
    // bean-init cycle with @Lazy. Spring injects a proxy and resolves the
    // real bean on first call.
    public UserAuthFetchAdapter(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthToken createToken(User user) {
        return authService.createToken(user);
    }
}
