package com.back.catchmate.inquiry.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryCreateResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryResult;
import com.back.catchmate.inquiry.presentation.dto.request.InquiryCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[사용자] 1:1 문의 API")
public interface InquiryApiDocs {

    @Operation(summary = "문의 등록", description = "새로운 1:1 문의를 등록합니다. (201)")
    ResponseEntity<InquiryCreateResult> createInquiry(
            @Parameter(hidden = true) Long userId, @Valid InquiryCreateRequest request);

    @Operation(summary = "문의 상세 조회", description = "문의 내용과 답변을 상세 조회합니다. 본인의 문의만 조회할 수 있습니다.")
    ResponseEntity<InquiryResult> getMyInquiry(@Parameter(hidden = true) Long userId, Long inquiryId);

    @Operation(summary = "내 문의 목록 조회", description = "로그인한 사용자의 1:1 문의 내역을 최신순으로 페이징 조회합니다.")
    ResponseEntity<OffsetPageResult<InquiryResult>> getMyInquiries(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);
}
