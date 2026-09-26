package com.back.catchmate.user.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.application.UserCommandService;
import com.back.catchmate.user.application.UserQueryService;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.presentation.dto.request.UserAlarmUpdateRequest;
import com.back.catchmate.user.presentation.dto.request.UserFcmTokenUpdateRequest;
import com.back.catchmate.user.presentation.dto.request.UserProfileUpdateRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiDocs {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<UserResult> getMyProfile(@AuthUser Long userId) {
        return ResponseEntity.ok(userQueryService.getMyProfile(userId));
    }

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserResult> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userQueryService.getUser(userId));
    }

    @Override
    @GetMapping("/nickname-availability")
    public ResponseEntity<NicknameAvailabilityResult> getNicknameAvailability(@RequestParam String nickName) {
        return ResponseEntity.ok(userQueryService.getNicknameAvailability(nickName));
    }

    @Override
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResult> updateMyProfile(
            @AuthUser Long userId,
            @Valid @RequestPart("request") UserProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage)
            throws IOException {
        // Application 이 Spring Web 의 MultipartFile 을 모르도록 표현 계층에서 UploadFile 로 바꾼다.
        UploadFile uploadFile = profileImage == null || profileImage.isEmpty()
                ? null
                : new UploadFile(
                        profileImage.getOriginalFilename(),
                        profileImage.getContentType(),
                        profileImage.getInputStream(),
                        profileImage.getSize());
        return ResponseEntity.ok(userCommandService.updateMyProfile(userId, request.toCommand(), uploadFile));
    }

    @Override
    @GetMapping("/me/alarms")
    public ResponseEntity<UserAlarmResult> getMyAlarms(@AuthUser Long userId) {
        return ResponseEntity.ok(userQueryService.getMyAlarms(userId));
    }

    @Override
    @PatchMapping("/me/alarms")
    public ResponseEntity<UserAlarmResult> updateMyAlarm(
            @AuthUser Long userId, @Valid @RequestBody UserAlarmUpdateRequest request) {
        return ResponseEntity.ok(userCommandService.updateMyAlarm(userId, request.toCommand()));
    }

    @Override
    @PutMapping("/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthUser Long userId, @Valid @RequestBody UserFcmTokenUpdateRequest request) {
        userCommandService.updateFcmToken(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }
}
