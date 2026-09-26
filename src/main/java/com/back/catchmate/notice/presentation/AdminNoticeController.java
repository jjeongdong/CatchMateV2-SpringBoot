package com.back.catchmate.notice.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.NoticeCommandService;
import com.back.catchmate.notice.application.NoticeQueryService;
import com.back.catchmate.notice.application.dto.result.NoticeCreateResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import com.back.catchmate.notice.presentation.dto.request.NoticeCreateRequest;
import com.back.catchmate.notice.presentation.dto.request.NoticeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/notices")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNoticeController implements AdminNoticeApiDocs {
    private final NoticeCommandService noticeCommandService;
    private final NoticeQueryService noticeQueryService;

    @Override
    @PostMapping
    public ResponseEntity<NoticeCreateResult> createNotice(
            @AuthUser Long userId, @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noticeCommandService.createNotice(userId, request.toCommand()));
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<NoticeResult>> getNotices(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(noticeQueryService.getNotices(page, size));
    }

    @Override
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResult> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeQueryService.getNotice(noticeId));
    }

    @Override
    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResult> updateNotice(
            @PathVariable Long noticeId, @RequestBody NoticeUpdateRequest request) {
        return ResponseEntity.ok(noticeCommandService.updateNotice(noticeId, request.toCommand()));
    }

    @Override
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeCommandService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
