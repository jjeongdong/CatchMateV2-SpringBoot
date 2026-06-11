package com.back.catchmate.auth.application.port.in;

import com.back.catchmate.auth.application.dto.response.AuthReissueResponse;

public interface AuthUseCase {
    Long getUserId(String token);
    String getUserRole(String token);
    AuthReissueResponse updateToken(String refreshToken);
    void deleteToken(String refreshToken);
}
