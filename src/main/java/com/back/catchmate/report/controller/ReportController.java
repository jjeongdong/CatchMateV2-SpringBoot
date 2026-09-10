package com.back.catchmate.report.controller;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.report.dto.request.ReportCreateRequest;
import com.back.catchmate.report.dto.response.ReportCreateResponse;
import com.back.catchmate.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 신고 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "신고 접수 API", description = "유저 신고를 접수하는 API 입니다.")
    public ResponseEntity<ReportCreateResponse> createReport(@AuthUser Long reporterId,
                                                             @Valid @RequestBody ReportCreateRequest request) {
        return ResponseEntity.ok(reportService.createReport(reporterId, request));
    }
}
