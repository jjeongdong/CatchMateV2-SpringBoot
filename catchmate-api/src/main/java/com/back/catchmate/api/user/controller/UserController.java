package com.back.catchmate.api.user.controller;

import com.back.catchmate.api.user.dto.UserRegisterRequest;
import com.back.catchmate.application.auth.dto.AuthLoginResponse;
import com.back.catchmate.application.user.UserUseCase;
import com.back.catchmate.application.user.dto.UserRegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[유저] 유저 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @PostMapping("/additional-info")
    @Operation(summary = "추가 정보 입력 API", description = "최초 로그인시, 추가 정보를 입력하는 API 입니다.")
    public UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return userUseCase.register(request.toCommand());
    }
}
