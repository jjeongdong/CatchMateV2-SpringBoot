package com.back.catchmate.auth.application.port.in;

import com.back.catchmate.auth.application.dto.response.IssuedAuthToken;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;

public interface AuthInternalCommandUseCase {
    /**
     * 사용자 ID와 권한으로 액세스/리프레시 토큰을 발급하고 저장.
     * cross-context 호출자는 User 엔티티/Authority enum 을 직접 넘기지 않는다.
     * 반환 타입은 도메인 모델 노출을 피하기 위해 record.
     */
    IssuedAuthToken createToken(Long userId, String authority);

    /**
     * Signup 토큰(아직 회원가입 전 1회용)을 발급한다.
     * 호출자는 자기 도메인을 노출하지 말고 primitive 값을 SignupTokenPayload 로 감싸 넘긴다.
     */
    String issueSignupToken(SignupTokenPayload payload);
}
