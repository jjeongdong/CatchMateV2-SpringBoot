package com.back.catchmate.auth.infrastructure.oauth;

import com.back.catchmate.auth.domain.OAuthClient;
import com.back.catchmate.auth.domain.OAuthProfile;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.exception.OAuthProviderException;
import com.back.catchmate.auth.infrastructure.oauth.dto.KakaoTokenResponse;
import com.back.catchmate.auth.infrastructure.oauth.dto.KakaoUserResponse;
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
public class KakaoOAuthClient implements OAuthClient {
    private final RestClient oauthRestClient;
    private final OAuthProperties properties;

    @Override
    public Provider supports() {
        return Provider.KAKAO;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.ProviderProperties kakao = properties.kakao();
        StringBuilder sb = new StringBuilder(kakao.authorizeUrl())
                .append("?response_type=code")
                .append("&client_id=")
                .append(UriUtils.encode(kakao.clientId(), StandardCharsets.UTF_8))
                .append("&redirect_uri=")
                .append(UriUtils.encode(kakao.redirectUri(), StandardCharsets.UTF_8))
                .append("&state=")
                .append(UriUtils.encode(state, StandardCharsets.UTF_8));
        if (kakao.scope() != null && !kakao.scope().isBlank()) {
            sb.append("&scope=").append(UriUtils.encode(kakao.scope(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public OAuthProfile exchange(String code) {
        KakaoTokenResponse token = requestToken(code);
        KakaoUserResponse user = requestUserInfo(token.accessToken());

        if (user.id() == null) {
            throw new OAuthProviderException();
        }
        return new OAuthProfile(Provider.KAKAO, String.valueOf(user.id()), user.email(), user.profileImageUrl());
    }

    private KakaoTokenResponse requestToken(String code) {
        OAuthProperties.ProviderProperties kakao = properties.kakao();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.clientId());
        form.add("redirect_uri", kakao.redirectUri());
        form.add("code", code);
        if (kakao.clientSecret() != null && !kakao.clientSecret().isBlank()) {
            form.add("client_secret", kakao.clientSecret());
        }

        log.info(
                "Kakao token 요청: url={}, client_id={}, redirect_uri={}, hasSecret={}",
                kakao.tokenUrl(),
                kakao.clientId(),
                kakao.redirectUri(),
                kakao.clientSecret() != null && !kakao.clientSecret().isBlank());
        try {
            KakaoTokenResponse response = oauthRestClient
                    .post()
                    .uri(kakao.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                log.error("Kakao token 응답 본문이 비어있음");
                throw new OAuthProviderException();
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Kakao token 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuthProviderException();
        } catch (RestClientException e) {
            log.error("Kakao token 요청 실패: {}", e.getMessage());
            throw new OAuthProviderException();
        }
    }

    private KakaoUserResponse requestUserInfo(String accessToken) {
        OAuthProperties.ProviderProperties kakao = properties.kakao();
        try {
            KakaoUserResponse response = oauthRestClient
                    .get()
                    .uri(kakao.userInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
            if (response == null) {
                throw new OAuthProviderException();
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Kakao userinfo 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuthProviderException();
        } catch (RestClientException e) {
            log.error("Kakao userinfo 요청 실패: {}", e.getMessage());
            throw new OAuthProviderException();
        }
    }
}
