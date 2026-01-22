package com.back.catchmate.domain.auth.service;

import com.back.catchmate.domain.auth.AuthToken;
import com.back.catchmate.domain.auth.repository.RefreshTokenRepository;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthToken issueToken(Long userId) {
        String accessToken = tokenProvider.createAccessToken(userId);
        String refreshToken = tokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(refreshToken, userId, tokenProvider.getRefreshTokenExpiration());
        return AuthToken.of(accessToken, refreshToken);
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
