package com.back.catchmate.oauth.adapter.out.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfig {

    /**
     * OAuth 공급자(카카오·구글) 호출 전용 RestClient.
     *
     * <p><b>타임아웃을 반드시 건다.</b> 기본 요청 팩토리는 connect/read 타임아웃이 무제한이라,
     * 공급자가 응답하지 않으면 로그인 요청을 처리하던 Tomcat 스레드가 무기한 park 된다.
     * 로그인 1회는 토큰 교환 + 사용자 조회를 순차로 호출하므로 장애가 나면 스레드 점유 시간이 두 배가 되고,
     * 로그인 트래픽이 계속 들어오면 스레드 풀이 말라 서비스 전체가 멈춘다.
     * 타임아웃 초과는 {@code ResourceAccessException}(= {@code RestClientException})으로 올라가
     * 각 클라이언트가 이미 {@code OAUTH_PROVIDER_ERROR} 로 변환한다.
     */
    @Bean
    public RestClient oauthRestClient(
            @Value("${oauth.client.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${oauth.client.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
