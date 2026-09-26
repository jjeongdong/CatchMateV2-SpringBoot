package com.back.catchmate.user.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryService;
import com.back.catchmate.user.application.dto.result.AdminUserDetailResult;
import com.back.catchmate.user.application.dto.result.AdminUserResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApiDocs {
    private final UserQueryService userQueryService;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResult> getAdminUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userQueryService.getAdminUser(userId));
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<AdminUserResult>> getAdminUsers(
            @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userQueryService.getAdminUsers(clubName, page, size));
    }
}
