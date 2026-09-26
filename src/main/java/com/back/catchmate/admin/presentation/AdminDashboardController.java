package com.back.catchmate.admin.presentation;

import com.back.catchmate.admin.application.AdminQueryService;
import com.back.catchmate.admin.application.dto.result.AdminDashboardResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController implements AdminDashboardApiDocs {
    private final AdminQueryService adminQueryService;

    @Override
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardResult> getDashboard() {
        return ResponseEntity.ok(adminQueryService.getDashboard());
    }
}
