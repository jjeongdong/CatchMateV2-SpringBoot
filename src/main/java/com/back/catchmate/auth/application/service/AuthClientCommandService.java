package com.back.catchmate.auth.application.service;

import com.back.catchmate.auth.application.dto.response.AuthReissueResponse;
import com.back.catchmate.auth.application.port.in.AuthClientCommandUseCase;
import com.back.catchmate.auth.application.port.out.persistence.RefreshTokenRepository;
import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.auth.application.port.out.external.UserCommandPort;
import com.back.catchmate.auth.application.port.out.external.UserFetchPort;
import com.back.catchmate.auth.application.port.out.dto.AuthUserInfo;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthClientCommandService implements AuthClientCommandUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    private final UserCommandPort userCommandPort;
    private final UserFetchPort userFetchPort;

    @Override
    public AuthReissueResponse updateToken(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));

        AuthUserInfo user = userFetchPort.getUser(userId);
        String newAccessToken = tokenProvider.createAccessToken(user.userId(), user.authority());
        return AuthReissueResponse.of(newAccessToken);
    }

    @Override
    public void deleteToken(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        userCommandPort.clearFcmToken(userId);
        refreshTokenRepository.deleteById(refreshToken);
    }
}
