package com.back.catchmate.auth.infrastructure.oauth;

import com.back.catchmate.auth.domain.OAuthClient;
import com.back.catchmate.auth.domain.OAuthProfile;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.exception.OAuthProviderException;
import com.back.catchmate.auth.infrastructure.oauth.dto.GoogleTokenResponse;
import com.back.catchmate.auth.infrastructure.oauth.dto.GoogleUserResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {
    private final RestClient oauthRestClient;
    private final OAuthProperties properties;

    @Override
    public Provider supports() {
        return Provider.GOOGLE;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.ProviderProperties google = properties.google();
        StringBuilder sb = new StringBuilder(google.authorizeUrl())
                .append("?response_type=code")
                .append("&client_id=")
                .append(UriUtils.encode(google.clientId(), StandardCharsets.UTF_8))
                .append("&redirect_uri=")
                .append(UriUtils.encode(google.redirectUri(), StandardCharsets.UTF_8))
                .append("&state=")
                .append(UriUtils.encode(state, StandardCharsets.UTF_8));
        if (google.scope() != null && !google.scope().isBlank()) {
            sb.append("&scope=").append(UriUtils.encode(google.scope(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public OAuthProfile exchange(String code) {
        GoogleTokenResponse token = requestToken(code);
        GoogleUserResponse user = requestUserInfo(token.accessToken());

        if (user.id() == null) {
            throw new OAuthProviderException();
        }
        return new OAuthProfile(Provider.GOOGLE, user.id(), user.email(), user.picture());
    }

    private GoogleTokenResponse requestToken(String code) {
        OAuthProperties.ProviderProperties google = properties.google();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", google.clientId());
        form.add("client_secret", google.clientSecret());
        form.add("redirect_uri", google.redirectUri());
        form.add("code", code);

        log.info(
                "Google token 요청: url={}, client_id={}, redirect_uri={}",
                google.tokenUrl(),
                google.clientId(),
                google.redirectUri());
        try {
            GoogleTokenResponse response = oauthRestClient
                    .post()
                    .uri(google.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                log.error("Google token 응답 본문이 비어있음");
                throw new OAuthProviderException();
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Google token 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuthProviderException();
        } catch (RestClientException e) {
            log.error("Google token 요청 실패: {}", e.getMessage());
            throw new OAuthProviderException();
        }
    }

    private GoogleUserResponse requestUserInfo(String accessToken) {
        OAuthProperties.ProviderProperties google = properties.google();
        try {
            GoogleUserResponse response = oauthRestClient
                    .get()
                    .uri(google.userInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserResponse.class);
            if (response == null) {
                throw new OAuthProviderException();
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Google userinfo 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuthProviderException();
        } catch (RestClientException e) {
            log.error("Google userinfo 요청 실패: {}", e.getMessage());
            throw new OAuthProviderException();
        }
    }
}
