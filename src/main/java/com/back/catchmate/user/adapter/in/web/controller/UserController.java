package com.back.catchmate.user.adapter.in.web.controller;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.user.adapter.in.web.dto.request.UserFcmTokenUpdateRequest;
import com.back.catchmate.common.upload.UploadFile;
import com.back.catchmate.user.adapter.in.web.dto.request.UserProfileUpdateRequest;
import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.application.port.in.UserClientCommandUseCase;
import com.back.catchmate.user.application.port.in.UserClientQueryUseCase;
import com.back.catchmate.user.domain.model.UserAlarmType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "[사용자] 유저 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserClientCommandUseCase userClientCommandUseCase;
    private final UserClientQueryUseCase userClientQueryUseCase;

    @GetMapping("/profile")
    @Operation(summary = "나의 정보 조회 API", description = "마이페이지에서 나의 모든 정보를 조회하는 API 입니다.")
    public ResponseEntity<UserResponse> getUserProfile(@Parameter(hidden = true) @AuthUser Long userId) {
        return ResponseEntity.ok(userClientQueryUseCase.getUserProfile(userId));
    }

    @GetMapping("/profile/{profileUserId}")
    @Operation(summary = "유저 정보 조회 API", description = "다른 유저의 정보를 조회하는 API 입니다.")
    public ResponseEntity<UserResponse> getUserProfileById(
            @Parameter(hidden = true) @AuthUser Long userId,
            @PathVariable Long profileUserId) {
        return ResponseEntity.ok(userClientQueryUseCase.getUserProfileById(userId, profileUserId));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인 API", description = "닉네임의 중복 여부를 확인하는 API 입니다.")
    public ResponseEntity<UserNicknameCheckResponse> getUserNicknameAvailability(@RequestParam("nickName") String nickName) {
        return ResponseEntity.ok(userClientQueryUseCase.getUserNicknameAvailability(nickName));
    }

    @PatchMapping(value = "/profile", consumes = "multipart/form-data")
    @Operation(summary = "나의 정보 수정 API", description = "마이페이지에서 나의 정보를 수정하는 API 입니다. (수정된 최신 유저 정보 반환)")
    public ResponseEntity<UserUpdateResponse> updateUserProfile(@Parameter(hidden = true) @AuthUser Long userId,
                                                                @RequestPart(value = "request", required = false) @Valid UserProfileUpdateRequest request,
                                                                @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        UploadFile uploadFile = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            uploadFile = new UploadFile(
                    profileImage.getOriginalFilename(),
                    profileImage.getContentType(),
                    profileImage.getInputStream(),
                    profileImage.getSize()
            );
        }

        return ResponseEntity.ok(userClientCommandUseCase.updateUserProfile(userId, request.toCommand(), uploadFile));
    }

    @PatchMapping("/alarm")
    @Operation(summary = "알림 설정 API", description = "유저의 알람 수신 여부를 변경하는 API 입니다.")
    public ResponseEntity<UserAlarmUpdateResponse> updateUserAlarm(@Parameter(hidden = true) @AuthUser Long userId,
                                                                   @RequestParam("alarmType") UserAlarmType alarmType,
                                                                   @RequestParam("isEnabled") boolean isEnabled) {
        return ResponseEntity.ok(userClientCommandUseCase.updateUserAlarm(userId, alarmType, isEnabled));
    }

    @GetMapping("/alarm")
    @Operation(summary = "알림 설정 조회 API", description = "알림 설정 페이지에서 유저의 알람 설정 상태를 조회하는 API 입니다.")
    public ResponseEntity<UserAlarmSettingsResponse> getUserAlarmSettings(@Parameter(hidden = true) @AuthUser Long userId) {
        return ResponseEntity.ok(userClientQueryUseCase.getUserAlarmSettings(userId));
    }

    @PutMapping("/me/fcm-token")
    @Operation(summary = "FCM 토큰 등록/갱신 API", description = "웹 푸시 사용을 위한 FCM 토큰을 등록 또는 갱신합니다.")
    public ResponseEntity<Void> updateFcmToken(@Parameter(hidden = true) @AuthUser Long userId,
                                               @Valid @RequestBody UserFcmTokenUpdateRequest request) {
        userClientCommandUseCase.updateUserFcmToken(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }
}
