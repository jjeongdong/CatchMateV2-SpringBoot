package com.back.catchmate.auth.application;

import com.back.catchmate.auth.domain.OAuthClient;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.exception.UnsupportedOAuthProviderException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OAuthClientRegistry {
    private final Map<Provider, OAuthClient> clientByProvider;

    public OAuthClientRegistry(List<OAuthClient> oauthClients) {
        Map<Provider, OAuthClient> map = new EnumMap<>(Provider.class);
        for (OAuthClient client : oauthClients) {
            map.put(client.supports(), client);
        }
        this.clientByProvider = map;
    }

    public OAuthClient get(Provider provider) {
        OAuthClient client = clientByProvider.get(provider);
        if (client == null) {
            throw new UnsupportedOAuthProviderException();
        }
        return client;
    }
}
