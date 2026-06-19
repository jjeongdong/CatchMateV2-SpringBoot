package com.back.catchmate.auth.application.service;

import com.back.catchmate.auth.application.port.in.AuthInternalQueryUseCase;
import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthInternalQueryService implements AuthInternalQueryUseCase {

    private final TokenProvider tokenProvider;

    @Override
    public Long getUserId(String token) {
        return tokenProvider.getUserId(token);
    }

    @Override
    public String getUserRole(String token) {
        return tokenProvider.getUserRole(token);
    }

    @Override
    public SignupTokenPayload parseSignupToken(String signupToken) {
        return tokenProvider.parseSignupToken(signupToken);
    }
}
