package com.back.catchmate.oauth.application.service;

import com.back.catchmate.oauth.application.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.application.port.in.OAuthClientQueryUseCase;
import com.back.catchmate.oauth.application.port.out.external.OAuthClient;
import com.back.catchmate.oauth.application.port.out.external.OAuthClientRegistry;
import com.back.catchmate.oauth.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthClientQueryService implements OAuthClientQueryUseCase {

    private final OAuthClientRegistry oauthClientRegistry;

    @Override
    public AuthorizeRedirect buildAuthorizeRedirect(Provider provider) {
        OAuthClient client = oauthClientRegistry.get(provider);
        String state = UUID.randomUUID().toString();
        String url = client.buildAuthorizeUrl(state);
        return new AuthorizeRedirect(url, state);
    }
}
