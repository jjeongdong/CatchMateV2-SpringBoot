package com.back.catchmate.api.user.controller;

import com.back.catchmate.api.user.dto.request.UserRegisterRequest;
import com.back.catchmate.application.user.UserUseCase;
import com.back.catchmate.application.user.dto.UserRegisterResponse;
import com.back.catchmate.application.user.dto.UserResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 유저 관련 API")
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

    @GetMapping("/profile")
    @Operation(summary = "나의 정보 조회 API", description = "마이페이지에서 나의 모든 정보를 조회하는 API 입니다.")
    public UserResponse getMyProfile(@AuthUser Long userId) {
        return userUseCase.getMyProfile(userId);
    }

    @GetMapping("/profile/{profileUserId}")
    @Operation(summary = "유저 정보 조회 API", description = "다른 유저의 정보를 조회하는 API 입니다.")
    public UserResponse getOtherUserProfile(@AuthUser Long userId,
                                            @PathVariable Long profileUserId) {
        return userUseCase.getOtherUserProfile(userId, profileUserId);
    }
}
