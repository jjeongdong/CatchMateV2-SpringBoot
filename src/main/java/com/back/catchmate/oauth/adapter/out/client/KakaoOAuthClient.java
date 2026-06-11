package com.back.catchmate.oauth.adapter.out.client;

import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.oauth.application.port.out.OAuthClient;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.adapter.out.config.OAuthProperties;
import com.back.catchmate.oauth.adapter.out.dto.KakaoTokenResponse;
import com.back.catchmate.oauth.adapter.out.dto.KakaoUserResponse;
import com.back.catchmate.user.domain.enums.Provider;
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
public class KakaoOAuthClient implements OAuthClient {
    private final RestClient oauthRestClient;
    private final OAuthProperties properties;

    @Override
    public Provider supports() {
        return Provider.KAKAO;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.ProviderProps kakao = properties.getKakao();
        StringBuilder sb = new StringBuilder(kakao.getAuthorizeUrl())
                .append("?response_type=code")
                .append("&client_id=").append(UriUtils.encode(kakao.getClientId(), StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(UriUtils.encode(kakao.getRedirectUri(), StandardCharsets.UTF_8))
                .append("&state=").append(UriUtils.encode(state, StandardCharsets.UTF_8));
        if (kakao.getScope() != null && !kakao.getScope().isBlank()) {
            sb.append("&scope=").append(UriUtils.encode(kakao.getScope(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public OAuthUserInfo exchange(String code) {
        KakaoTokenResponse token = requestToken(code);
        KakaoUserResponse userInfo = requestUserInfo(token.getAccessToken());

        if (userInfo.getId() == null) {
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }

        return OAuthUserInfo.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(userInfo.getId()))
                .email(userInfo.getEmail())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .nickname(userInfo.getNickname())
                .build();
    }

    private KakaoTokenResponse requestToken(String code) {
        OAuthProperties.ProviderProps kakao = properties.getKakao();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.getClientId());
        form.add("redirect_uri", kakao.getRedirectUri());
        form.add("code", code);
        if (kakao.getClientSecret() != null && !kakao.getClientSecret().isBlank()) {
            form.add("client_secret", kakao.getClientSecret());
        }

        log.info("Kakao token 요청: url={}, client_id={}, redirect_uri={}, hasSecret={}",
                kakao.getTokenUrl(), kakao.getClientId(), kakao.getRedirectUri(),
                kakao.getClientSecret() != null && !kakao.getClientSecret().isBlank());
        try {
            KakaoTokenResponse response = oauthRestClient.post()
                    .uri(kakao.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (response == null || response.getAccessToken() == null) {
                log.error("Kakao token 응답 본문이 비어있음");
                throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Kakao token 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("Kakao token 요청 실패: {}", e.getMessage());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    private KakaoUserResponse requestUserInfo(String accessToken) {
        OAuthProperties.ProviderProps kakao = properties.getKakao();
        try {
            KakaoUserResponse response = oauthRestClient.get()
                    .uri(kakao.getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
            if (response == null) {
                throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Kakao userinfo 요청 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("Kakao userinfo 요청 실패: {}", e.getMessage());
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
