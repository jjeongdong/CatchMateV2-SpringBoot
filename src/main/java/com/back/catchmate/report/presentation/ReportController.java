package com.back.catchmate.report.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.report.application.ReportCommandService;
import com.back.catchmate.report.application.dto.result.ReportCreateResult;
import com.back.catchmate.report.presentation.dto.request.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController implements ReportApiDocs {
    private final ReportCommandService reportCommandService;

    @Override
    @PostMapping
    public ResponseEntity<ReportCreateResult> createReport(
            @AuthUser Long reporterId, @RequestBody ReportCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportCommandService.createReport(reporterId, request.toCommand()));
    }
}
