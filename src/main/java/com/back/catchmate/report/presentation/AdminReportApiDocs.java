package com.back.catchmate.report.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.report.application.dto.result.ReportDetailResult;
import com.back.catchmate.report.application.dto.result.ReportProcessResult;
import com.back.catchmate.report.application.dto.result.ReportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[관리자] 신고 API")
public interface AdminReportApiDocs {

    @Operation(summary = "관리자 신고 목록 조회", description = "전체 신고 내역을 최신순으로 페이징 조회합니다.")
    ResponseEntity<OffsetPageResult<ReportResult>> getReports(@PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "관리자 신고 상세 조회", description = "특정 신고 내역의 상세 정보를 조회합니다.")
    ResponseEntity<ReportDetailResult> getReport(Long reportId);

    @Operation(summary = "신고 처리", description = "별도의 입력 정보 없이, 해당 신고 건을 처리하고 신고 당한 유저를 '신고됨(true)' 상태로 변경합니다.")
    ResponseEntity<ReportProcessResult> processReport(Long reportId);
}
