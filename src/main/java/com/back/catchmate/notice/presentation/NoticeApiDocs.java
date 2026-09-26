package com.back.catchmate.notice.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다: 구현 메서드가 인터페이스 메서드의 파라미터 제약을 다시 선언하면
// Hibernate Validator 가 메서드 검증 시 HV000151 을 던진다. Spring MVC 는 인터페이스 파라미터 어노테이션도 읽는다.
@Tag(name = "[사용자] 공지사항 API")
public interface NoticeApiDocs {

    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    ResponseEntity<NoticeDetailResult> getNotice(Long noticeId);

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 최신순으로 페이징 조회합니다. (page 는 0부터, size 기본 20·최대 100)")
    ResponseEntity<OffsetPageResult<NoticeResult>> getNotices(@PositiveOrZero int page, @Min(1) @Max(100) int size);
}
