package com.back.catchmate.auth.service;

import com.back.catchmate.auth.dto.SignupTokenPayload;
import com.back.catchmate.auth.dto.response.AuthReissueResponse;
import com.back.catchmate.auth.dto.response.IssuedAuthToken;
import com.back.catchmate.auth.infra.JwtTokenProvider;
import com.back.catchmate.auth.infra.RefreshTokenRedisRepository;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 클래스 레벨에 @Transactional 을 붙이지 말 것.
// 토큰 파싱/검증 메서드(getUserId·getUserRole·parseSignupToken)는 순수 JWT 연산이라 DB 트랜잭션이 필요 없다.
// 클래스 레벨에 붙이면 인증 필터가 매 요청 트랜잭션을 열어 커넥션 풀에 엮이고,
// 부하 시 풀 고갈이 INVALID_TOKEN(401)으로 잘못 표면화된다. 트랜잭션은 메서드 단위로만 선언한다.
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    private final UserService userService;

    @Transactional
    public AuthReissueResponse updateToken(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        refreshTokenRepository
                .findById(refreshToken)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));

        // refresh token 의 role 클레임을 믿지 않고 최신 권한을 다시 조회한다.
        // (권한이 강등된 사용자가 옛 refresh token 으로 옛 권한을 재발급받는 것을 막는다.)
        UserSummary user = userService.getUserSummary(userId);
        String newAccessToken = tokenProvider.createAccessToken(user.userId(), user.authority());
        return AuthReissueResponse.of(newAccessToken);
    }

    @Transactional
    public void deleteToken(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        userService.clearFcmToken(userId);
        refreshTokenRepository.deleteById(refreshToken);
    }

    @Transactional
    public IssuedAuthToken createToken(Long userId, String authority) {
        String accessToken = tokenProvider.createAccessToken(userId, authority);
        String refreshToken = tokenProvider.createRefreshToken(userId, authority);

        refreshTokenRepository.save(refreshToken, userId, tokenProvider.getRefreshTokenExpirationTime());
        return new IssuedAuthToken(accessToken, refreshToken);
    }

    @Transactional
    public String issueSignupToken(SignupTokenPayload payload) {
        return tokenProvider.createSignupToken(payload);
    }

    public Long getUserId(String token) {
        return tokenProvider.getUserId(token);
    }

    public String getUserRole(String token) {
        return tokenProvider.getUserRole(token);
    }

    public SignupTokenPayload parseSignupToken(String signupToken) {
        return tokenProvider.parseSignupToken(signupToken);
    }
}
