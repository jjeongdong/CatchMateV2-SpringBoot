package com.back.catchmate.api.notice.controller;

import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.notice.NoticeUseCase;
import com.back.catchmate.application.notice.dto.response.NoticeDetailResponse;
import com.back.catchmate.application.notice.dto.response.NoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 공지사항 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeUseCase noticeUseCase;

    @GetMapping("/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    public ResponseEntity<NoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeUseCase.getNotice(noticeId));
    }

    @GetMapping
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다. (page는 0부터 시작)")
    public ResponseEntity<PagedResponse<NoticeResponse>> getNoticeList(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeUseCase.getNoticeList(page, size));
    }
}
