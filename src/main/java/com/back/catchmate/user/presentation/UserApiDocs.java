package com.back.catchmate.user.presentation;

import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.presentation.dto.request.UserAlarmUpdateRequest;
import com.back.catchmate.user.presentation.dto.request.UserFcmTokenUpdateRequest;
import com.back.catchmate.user.presentation.dto.request.UserProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "[사용자] 유저 관련 API")
public interface UserApiDocs {

    @Operation(summary = "나의 정보 조회 API", description = "마이페이지에서 나의 모든 정보를 조회하는 API 입니다.")
    ResponseEntity<UserResult> getMyProfile(@Parameter(hidden = true) Long userId);

    @Operation(summary = "유저 정보 조회 API", description = "다른 유저의 정보를 조회하는 API 입니다.")
    ResponseEntity<UserResult> getUser(Long userId);

    @Operation(summary = "닉네임 사용 가능 여부 조회 API", description = "닉네임을 사용할 수 있는지(중복이 아닌지) 확인하는 API 입니다.")
    ResponseEntity<NicknameAvailabilityResult> getNicknameAvailability(String nickName);

    @Operation(
            summary = "나의 정보 수정 API",
            description = "마이페이지에서 나의 정보를 수정하는 API 입니다. multipart 의 request 파트는 필수이며(바꿀 값이 없으면 {}), "
                    + "profileImage 파트는 선택입니다. 수정된 최신 유저 정보를 반환합니다.")
    ResponseEntity<UserResult> updateMyProfile(
            @Parameter(hidden = true) Long userId, UserProfileUpdateRequest request, MultipartFile profileImage)
            throws IOException;

    @Operation(summary = "알림 설정 조회 API", description = "알림 설정 페이지에서 유저의 알람 설정 상태를 조회하는 API 입니다.")
    ResponseEntity<UserAlarmResult> getMyAlarms(@Parameter(hidden = true) Long userId);

    @Operation(summary = "알림 설정 API", description = "유저의 알람 수신 여부를 변경하고, 변경 후 전체 알림 설정을 반환하는 API 입니다.")
    ResponseEntity<UserAlarmResult> updateMyAlarm(
            @Parameter(hidden = true) Long userId, UserAlarmUpdateRequest request);

    @Operation(summary = "FCM 토큰 등록/갱신 API", description = "웹 푸시 사용을 위한 FCM 토큰을 등록 또는 갱신합니다.")
    ResponseEntity<Void> updateFcmToken(@Parameter(hidden = true) Long userId, UserFcmTokenUpdateRequest request);
}
