package com.back.catchmate.notice.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.NoticeQueryService;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController implements NoticeApiDocs {
    private final NoticeQueryService noticeQueryService;

    @Override
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResult> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeQueryService.getNotice(noticeId));
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<NoticeResult>> getNotices(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(noticeQueryService.getNotices(page, size));
    }
}
