package com.back.catchmate.auth.application.port.in;

import com.back.catchmate.auth.application.dto.response.AuthReissueResponse;

public interface AuthClientCommandUseCase {
    AuthReissueResponse updateToken(String refreshToken);

    void deleteToken(String refreshToken);
}
