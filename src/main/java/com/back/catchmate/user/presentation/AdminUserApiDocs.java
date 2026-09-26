package com.back.catchmate.user.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.AdminUserDetailResult;
import com.back.catchmate.user.application.dto.result.AdminUserResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[관리자] 회원 관련 API")
public interface AdminUserApiDocs {

    @Operation(summary = "회원 상세 조회", description = "특정 회원의 상세 정보를 조회합니다.")
    ResponseEntity<AdminUserDetailResult> getAdminUser(Long userId);

    @Operation(summary = "회원 목록 조회", description = "구단명으로 거른 회원 목록을 최신 가입순으로 조회합니다. 구단명이 없으면 전체, 없는 구단이면 빈 목록입니다.")
    ResponseEntity<OffsetPageResult<AdminUserResult>> getAdminUsers(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)") String clubName,
            @PositiveOrZero int page,
            @Min(1) @Max(100) int size);
}
