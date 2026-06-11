package com.back.catchmate.infrastructure.oauth.client;

import com.back.catchmate.domain.oauth.port.OAuthClient;
import com.back.catchmate.domain.oauth.port.OAuthClientRegistry;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import com.back.catchmate.user.enums.Provider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthClientRegistryImpl implements OAuthClientRegistry {
    private final Map<Provider, OAuthClient> clients;

    public OAuthClientRegistryImpl(List<OAuthClient> oauthClients) {
        Map<Provider, OAuthClient> map = new EnumMap<>(Provider.class);
        for (OAuthClient client : oauthClients) {
            map.put(client.supports(), client);
        }
        this.clients = map;
    }

    @Override
    public OAuthClient get(Provider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BaseException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return client;
    }
}
