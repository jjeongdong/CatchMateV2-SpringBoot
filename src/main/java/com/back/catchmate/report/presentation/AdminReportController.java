package com.back.catchmate.report.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.report.application.ReportCommandService;
import com.back.catchmate.report.application.ReportQueryService;
import com.back.catchmate.report.application.dto.result.ReportDetailResult;
import com.back.catchmate.report.application.dto.result.ReportProcessResult;
import com.back.catchmate.report.application.dto.result.ReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController implements AdminReportApiDocs {
    private final ReportCommandService reportCommandService;
    private final ReportQueryService reportQueryService;

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<ReportResult>> getReports(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportQueryService.getReports(page, size));
    }

    @Override
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResult> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportQueryService.getReport(reportId));
    }

    @Override
    @PostMapping("/{reportId}/process")
    public ResponseEntity<ReportProcessResult> processReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportCommandService.processReport(reportId));
    }
}
