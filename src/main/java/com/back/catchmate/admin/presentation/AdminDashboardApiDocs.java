package com.back.catchmate.admin.presentation;

import com.back.catchmate.admin.application.dto.result.AdminDashboardResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "[관리자] 대시보드 API")
public interface AdminDashboardApiDocs {

    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    ResponseEntity<AdminDashboardResult> getDashboard();
}
