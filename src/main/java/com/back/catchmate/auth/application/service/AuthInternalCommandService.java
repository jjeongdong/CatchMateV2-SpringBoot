package com.back.catchmate.auth.application.service;

import com.back.catchmate.auth.application.dto.response.IssuedAuthToken;
import com.back.catchmate.auth.application.port.in.AuthInternalCommandUseCase;
import com.back.catchmate.auth.application.port.out.persistence.RefreshTokenRepository;
import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthInternalCommandService implements AuthInternalCommandUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    @Override
    public IssuedAuthToken createToken(Long userId, String authority) {
        String accessToken = tokenProvider.createAccessToken(userId, authority);
        String refreshToken = tokenProvider.createRefreshToken(userId, authority);

        refreshTokenRepository.save(
                refreshToken,
                userId,
                tokenProvider.getRefreshTokenExpirationTime()
        );
        return new IssuedAuthToken(accessToken, refreshToken);
    }

    @Override
    public String issueSignupToken(SignupTokenPayload payload) {
        return tokenProvider.createSignupToken(payload);
    }
}
