package com.back.catchmate.domain.auth.service;

import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.port.TokenProvider;
import com.back.catchmate.domain.auth.repository.RefreshTokenRepository;
import com.back.catchmate.domain.inquiry.service.InquiryService;
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

    public Long extractUserIdFromToken(String token) {
        return tokenProvider.parseUserId(token);
    }

    public Long extractUserIdFromRefreshToken(String refreshToken) {
        return tokenProvider.parseUserId(refreshToken);
    }

    public String extractUserRoleFromToken(String token) {
        return tokenProvider.getRole(token);
    }

    public AuthToken issueToken(User user) {
        String accessToken = issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        refreshTokenRepository.save(
                refreshToken,
                user.getId(),
                tokenProvider.getRefreshTokenExpiration()
        );
        return AuthToken.createToken(accessToken, refreshToken);
    }

    public String issueAccessToken(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getAuthority());
    }

    private String issueRefreshToken(User user) {
        return tokenProvider.createRefreshToken(user.getId(), user.getAuthority());
    }

    public void validateRefreshTokenExistence(String refreshToken) {
        refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    public AuthToken login(User user, String fcmToken) {
        user.updateFcmToken(fcmToken);

        // 토큰 발급
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getAuthority());
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), user.getAuthority());

        refreshTokenRepository.save(refreshToken, user.getId(), tokenProvider.getRefreshTokenExpiration());
        return AuthToken.createToken(accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }
}
