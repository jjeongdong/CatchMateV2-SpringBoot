package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.oauth.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public Optional<User> findByProviderId(String providerIdWithProvider) {
        return userService.findByProviderId(providerIdWithProvider);
    }
}
