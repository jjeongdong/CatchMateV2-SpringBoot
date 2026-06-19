package com.back.catchmate.auth.application.port.in;

import com.back.catchmate.auth.application.dto.SignupTokenPayload;

public interface AuthInternalQueryUseCase {
    Long getUserId(String token);

    String getUserRole(String token);

    /**
     * Signup 토큰을 파싱하여 안에 담긴 primitive 정보를 반환한다.
     */
    SignupTokenPayload parseSignupToken(String signupToken);
}
