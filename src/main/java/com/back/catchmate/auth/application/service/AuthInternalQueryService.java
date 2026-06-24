package com.back.catchmate.auth.application.service;

import com.back.catchmate.auth.application.port.in.AuthInternalQueryUseCase;
import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 순수 JWT 파싱/검증만 위임하므로 DB 트랜잭션이 필요 없다.
// (@Transactional 을 붙이면 인증 필터가 매 요청 트랜잭션을 열어 커넥션 풀에 엮이고,
//  부하 시 풀 고갈이 INVALID_TOKEN(401)으로 잘못 표면화된다.)
@Service
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
