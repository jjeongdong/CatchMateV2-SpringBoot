package com.back.catchmate.application.auth;

import com.back.catchmate.application.auth.dto.command.AuthLoginCommand;
import com.back.catchmate.application.auth.dto.response.AuthLoginResponse;
import com.back.catchmate.application.auth.dto.response.AuthReissueResponse;
import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.service.AuthService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthUseCase {
    private final UserService userService;
    private final AuthService authService;

    @Transactional
    public AuthLoginResponse createToken(AuthLoginCommand command) {
        // 1. 유저 조회
        Optional<User> userOptional = userService.findByProviderId(command.getProviderIdWithProvider());

        // 2. 유저가 존재하지 않으면 회원가입 필요
        if (userOptional.isEmpty()) {
            return AuthLoginResponse.of(null, null, true);
        }

        // 3. 유저가 존재하면 로그인 성공
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
