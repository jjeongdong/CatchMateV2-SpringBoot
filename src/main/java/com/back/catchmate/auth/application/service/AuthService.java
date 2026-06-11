package com.back.catchmate.auth.application.service;

import com.back.catchmate.auth.application.port.out.UserFetchPort;

import com.back.catchmate.auth.application.dto.response.AuthReissueResponse;
import com.back.catchmate.auth.application.port.in.AuthUseCase;
import com.back.catchmate.auth.application.port.out.RefreshTokenRepository;
import com.back.catchmate.auth.application.port.out.TokenProvider;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final UserFetchPort userFetchPort;

    public Long getUserId(String token) {
        return getUserIdFromToken(token);
    }

    public String getUserRole(String token) {
        return getUserRoleFromToken(token);
    }

    @Transactional
    public AuthReissueResponse updateToken(String refreshToken) {
        Long userId = getUserIdFromRefreshToken(refreshToken);
        validateRefreshTokenExistence(refreshToken);

        User user = userFetchPort.getUser(userId);
        String newAccessToken = createAccessToken(user);
        return AuthReissueResponse.of(newAccessToken);
    }

    @Transactional
    public void deleteToken(String refreshToken) {
        Long userId = getUserIdFromRefreshToken(refreshToken);
        User user = userFetchPort.getUser(userId);

        user.deleteFcmToken();
        userFetchPort.updateUser(user);

        revokeRefreshToken(refreshToken);
    }


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

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }
}
