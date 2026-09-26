package com.back.catchmate.notice.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.dto.result.NoticeCreateResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import com.back.catchmate.notice.presentation.dto.request.NoticeCreateRequest;
import com.back.catchmate.notice.presentation.dto.request.NoticeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (NoticeApiDocs 와 같은 이유 — HV000151).
@Tag(name = "[관리자] 공지사항 API")
public interface AdminNoticeApiDocs {

    @Operation(summary = "공지사항 등록", description = "공지사항을 등록합니다. (관리자 전용, 201)")
    ResponseEntity<NoticeCreateResult> createNotice(
            @Parameter(hidden = true) Long userId, @Valid NoticeCreateRequest request);

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 최신순으로 페이징 조회합니다. (관리자 전용)")
    ResponseEntity<OffsetPageResult<NoticeResult>> getNotices(@PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다. (관리자 전용)")
    ResponseEntity<NoticeDetailResult> getNotice(Long noticeId);

    @Operation(summary = "공지사항 수정", description = "특정 공지사항을 수정하고 수정된 상세를 반환합니다. (관리자 전용)")
    ResponseEntity<NoticeDetailResult> updateNotice(Long noticeId, @Valid NoticeUpdateRequest request);

    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다. (관리자 전용, 204)")
    ResponseEntity<Void> deleteNotice(Long noticeId);
}
