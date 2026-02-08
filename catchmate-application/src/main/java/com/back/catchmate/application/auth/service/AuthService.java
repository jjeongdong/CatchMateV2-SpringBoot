package com.back.catchmate.application.auth.service;

import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.port.TokenProvider;
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

    public AuthToken createToken(User user) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        refreshTokenRepository.save(
                refreshToken,
                user.getId(),
                tokenProvider.getRefreshTokenExpirationTime()
        );
        return AuthToken.createToken(accessToken, refreshToken);
    }

    public AuthToken createToken(User user, String fcmToken) {
        user.updateFcmToken(fcmToken);

        // 토큰 발급
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getAuthority());
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), user.getAuthority());

        refreshTokenRepository.save(refreshToken, user.getId(), tokenProvider.getRefreshTokenExpirationTime());
        return AuthToken.createToken(accessToken, refreshToken);
    }

    public String createAccessToken(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getAuthority());
    }

    private String createRefreshToken(User user) {
        return tokenProvider.createRefreshToken(user.getId(), user.getAuthority());
    }

    public Long getUserIdFromToken(String token) {
        return tokenProvider.getUserId(token);
    }

    public Long getUserIdFromRefreshToken(String refreshToken) {
        return tokenProvider.getUserId(refreshToken);
    }

    public String getUserRoleFromToken(String token) {
        return tokenProvider.getUserRole(token);
    }

    public void validateRefreshTokenExistence(String refreshToken) {
        refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    public void deleteToken(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }
}
