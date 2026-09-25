package com.back.catchmate.oauth.infra;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.entity.Provider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OAuthClientRegistry {
    private final Map<Provider, OAuthClient> clients;

    public OAuthClientRegistry(List<OAuthClient> oauthClients) {
        Map<Provider, OAuthClient> map = new EnumMap<>(Provider.class);
        for (OAuthClient client : oauthClients) {
            map.put(client.supports(), client);
        }
        this.clients = map;
    }

    public OAuthClient get(Provider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BaseException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return client;
    }
}
