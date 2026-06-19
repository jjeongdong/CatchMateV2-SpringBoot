package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.oauth.application.dto.response.RegisteredUserSummary;
import com.back.catchmate.oauth.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OauthUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public Optional<RegisteredUserSummary> findByProviderId(String providerIdWithProvider) {
        return userInternalQueryUseCase.findByProviderId(providerIdWithProvider)
                .map(user -> new RegisteredUserSummary(user.userId(), user.authority()));
    }
}
