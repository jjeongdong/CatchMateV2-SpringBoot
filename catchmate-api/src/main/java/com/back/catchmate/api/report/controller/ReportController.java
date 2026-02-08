package com.back.catchmate.api.report.controller;

import com.back.catchmate.api.report.dto.request.ReportCreateRequest;
import com.back.catchmate.global.annotation.AuthUser;
import com.back.catchmate.orchestration.report.ReportOrchestrator;
import com.back.catchmate.orchestration.report.dto.response.ReportCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 유저 신고 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportOrchestrator reportOrchestrator;

    @PostMapping
    @Operation(summary = "유저 신고", description = "특정 유저를 신고합니다.")
    public ResponseEntity<ReportCreateResponse> createReport(@AuthUser Long userId,
                                                             @RequestBody @Valid ReportCreateRequest request) {
        return ResponseEntity.ok(reportOrchestrator.createReport(userId, request.toCommand()));
    }
}
