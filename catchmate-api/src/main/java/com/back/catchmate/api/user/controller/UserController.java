package com.back.catchmate.api.user.controller;

import com.back.catchmate.api.user.dto.request.UserProfileUpdateRequest;
import com.back.catchmate.api.user.dto.request.UserRegisterRequest;
import com.back.catchmate.application.user.UserUseCase;
import com.back.catchmate.application.user.dto.UploadFile;
import com.back.catchmate.application.user.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.application.user.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.application.user.dto.response.UserRegisterResponse;
import com.back.catchmate.application.user.dto.response.UserResponse;
import com.back.catchmate.application.user.dto.response.UserUpdateResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import user.enums.AlarmType;

import java.io.IOException;

@Tag(name = "[사용자] 유저 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @PostMapping("/additional-info")
    @Operation(summary = "추가 정보 입력 API", description = "최초 로그인시, 추가 정보를 입력하는 API 입니다.")
    public ResponseEntity<UserRegisterResponse> createUser(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(userUseCase.createUser(request.toCommand()));
    }

    @GetMapping("/profile")
    @Operation(summary = "나의 정보 조회 API", description = "마이페이지에서 나의 모든 정보를 조회하는 API 입니다.")
    public ResponseEntity<UserResponse> getUserProfile(@AuthUser Long userId) {
        return ResponseEntity.ok(userUseCase.getUserProfile(userId));
    }

    @GetMapping("/profile/{profileUserId}")
    @Operation(summary = "유저 정보 조회 API", description = "다른 유저의 정보를 조회하는 API 입니다.")
    public ResponseEntity<UserResponse> getUserProfileById(@AuthUser Long userId,
                                                           @PathVariable Long profileUserId) {
        return ResponseEntity.ok(userUseCase.getUserProfileById(userId, profileUserId));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인 API", description = "닉네임의 중복 여부를 확인하는 API 입니다.")
    public ResponseEntity<UserNicknameCheckResponse> getUserNicknameAvailability(@RequestParam("nickName") String nickName) {
        return ResponseEntity.ok(userUseCase.getUserNicknameAvailability(nickName));
    }

    @PatchMapping(value = "/profile", consumes = "multipart/form-data")
    @Operation(summary = "나의 정보 수정 API", description = "마이페이지에서 나의 정보를 수정하는 API 입니다. (수정된 최신 유저 정보 반환)")
    public ResponseEntity<UserUpdateResponse> updateUserProfile(@AuthUser Long userId,
                                                                @RequestPart(value = "request", required = false) @Valid UserProfileUpdateRequest request,
                                                                @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        UploadFile uploadFile = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            uploadFile = UploadFile.builder()
                    .originalFilename(profileImage.getOriginalFilename())
                    .contentType(profileImage.getContentType())
                    .size(profileImage.getSize())
                    .inputStream(profileImage.getInputStream())
                    .build();
        }

        return ResponseEntity.ok(userUseCase.updateUserProfile(userId, UserProfileUpdateRequest.toCommand(request), uploadFile));
    }

    @PatchMapping("/alarm")
    @Operation(summary = "알림 설정 API", description = "유저의 알람 수신 여부를 변경하는 API 입니다.")
    public ResponseEntity<UserAlarmUpdateResponse> updateUserAlarm(@AuthUser Long userId,
                                                                   @RequestParam("alarmType") AlarmType alarmType,
                                                                   @RequestParam("isEnabled") boolean isEnabled) {
        return ResponseEntity.ok(userUseCase.updateUserAlarm(userId, alarmType, isEnabled));
    }
}
