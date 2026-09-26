package com.back.catchmate.report.presentation;

import com.back.catchmate.report.application.dto.result.ReportCreateResult;
import com.back.catchmate.report.presentation.dto.request.ReportCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[사용자] 신고 관련 API")
public interface ReportApiDocs {

    @Operation(summary = "신고 접수 API", description = "유저 신고를 접수하는 API 입니다. (201)")
    ResponseEntity<ReportCreateResult> createReport(
            @Parameter(hidden = true) Long reporterId, @Valid ReportCreateRequest request);
}
