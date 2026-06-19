package com.back.catchmate.oauth.adapter.out.client;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.adapter.out.config.OAuthProperties;
import com.back.catchmate.oauth.adapter.out.dto.GoogleTokenResponse;
import com.back.catchmate.oauth.adapter.out.dto.GoogleUserResponse;
import com.back.catchmate.oauth.application.port.out.external.OAuthClient;
import com.back.catchmate.oauth.domain.enums.Provider;
import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
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

import java.nio.charset.StandardCharsets;

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
        OAuthProperties.ProviderProps google = properties.getGoogle();
        StringBuilder sb = new StringBuilder(google.getAuthorizeUrl())
                .append("?response_type=code")
                .append("&client_id=").append(UriUtils.encode(google.getClientId(), StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(UriUtils.encode(google.getRedirectUri(), StandardCharsets.UTF_8))
                .append("&state=").append(UriUtils.encode(state, StandardCharsets.UTF_8));
        if (google.getScope() != null && !google.getScope().isBlank()) {
            sb.append("&scope=").append(UriUtils.encode(google.getScope(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public OAuthUserInfo exchange(String code) {
        GoogleTokenResponse token = requestToken(code);
        GoogleUserResponse userInfo = requestUserInfo(token.getAccessToken());

        if (userInfo.getId() == null) {
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }

        return OAuthUserInfo.builder()
                .provider(Provider.GOOGLE)
                .providerId(userInfo.getId())
                .email(userInfo.getEmail())
                .profileImageUrl(userInfo.getPicture())
                .nickname(userInfo.getName())
                .build();
    }

    private GoogleTokenResponse requestToken(String code) {
        OAuthProperties.ProviderProps google = properties.getGoogle();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", google.getClientId());
        form.add("client_secret", google.getClientSecret());
        form.add("redirect_uri", google.getRedirectUri());
        form.add("code", code);

        log.info("Google token 요청: url={}, client_id={}, redirect_uri={}",
                google.getTokenUrl(), google.getClientId(), google.getRedirectUri());
        try {
            GoogleTokenResponse response = oauthRestClient.post()
                    .uri(google.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || response.getAccessToken() == null) {
                log.error("Google token 응답 본문이 비어있음");
                throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Google token 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("Google token 요청 실패: {}", e.getMessage());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    private GoogleUserResponse requestUserInfo(String accessToken) {
        OAuthProperties.ProviderProps google = properties.getGoogle();
        try {
            GoogleUserResponse response = oauthRestClient.get()
                    .uri(google.getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserResponse.class);
            if (response == null) {
                throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Google userinfo 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("Google userinfo 요청 실패: {}", e.getMessage());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
