package com.back.catchmate.domain.auth.service;

import com.back.catchmate.domain.auth.AuthToken;
import com.back.catchmate.domain.auth.repository.RefreshTokenRepository;
import com.back.catchmate.domain.user.model.User;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public Long extractUserIdFromRefreshToken(String refreshToken) {
        return tokenProvider.parseUserId(refreshToken);
    }

    public AuthToken issueToken(Long userId) {
        String accessToken = issueAccessToken(userId);
        String refreshToken = issueRefreshToken(userId);

        refreshTokenRepository.save(
                refreshToken,
                userId,
                tokenProvider.getRefreshTokenExpiration()
        );
        return AuthToken.of(accessToken, refreshToken);
    }

    public String issueAccessToken(Long userId) {
        return tokenProvider.createAccessToken(userId);
    }

    private String issueRefreshToken(Long userId) {
        return tokenProvider.createRefreshToken(userId);
    }

    public void validateRefreshTokenExistence(String refreshToken) {
        refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    public AuthToken login(User user, String fcmToken) {
        user.updateFcmToken(fcmToken);

        // 2. 토큰 발급
        String accessToken = tokenProvider.createAccessToken(user.getId());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(refreshToken, user.getId(), tokenProvider.getRefreshTokenExpiration());
        return AuthToken.of(accessToken, refreshToken);
    }
}
