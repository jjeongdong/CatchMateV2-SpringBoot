package com.back.catchmate.orchestration.auth;

import com.back.catchmate.application.auth.service.AuthService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.auth.dto.command.AuthLoginCommand;
import com.back.catchmate.orchestration.auth.dto.response.AuthLoginResponse;
import com.back.catchmate.orchestration.auth.dto.response.AuthReissueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthOrchestrator {
    private final AuthService authService;
    private final UserService userService;

    @Transactional
    public AuthLoginResponse createToken(AuthLoginCommand command) {
        Optional<User> userOptional = userService.findByProviderId(command.getProviderIdWithProvider());
        if (userOptional.isEmpty()) {
            return AuthLoginResponse.of(null, null, true);
        }

        User user = userOptional.get();
        AuthToken token = authService.createToken(user, command.getFcmToken());
        return AuthLoginResponse.of(token.getAccessToken(), token.getRefreshToken(), false);
    }

    public Long getUserId(String token) {
        return authService.getUserIdFromToken(token);
    }

    public String getUserRole(String token) {
        return authService.getUserRoleFromToken(token);
    }

    @Transactional
    public AuthReissueResponse updateToken(String refreshToken) {
        Long userId = authService.getUserIdFromRefreshToken(refreshToken);
        authService.validateRefreshTokenExistence(refreshToken);

        User user = userService.getUser(userId);
        String newAccessToken = authService.createAccessToken(user);
        return AuthReissueResponse.of(newAccessToken);
    }

    @Transactional
    public void deleteToken(String refreshToken) {
        Long userId = authService.getUserIdFromRefreshToken(refreshToken);
        User user = userService.getUser(userId);

        user.deleteFcmToken();
        userService.updateUser(user);

        authService.deleteToken(refreshToken);
    }
}
