package com.back.catchmate.inquiry.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryDetailResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryResult;
import com.back.catchmate.inquiry.application.dto.result.AnswerDraftResult;
import com.back.catchmate.inquiry.application.dto.result.CorpusReindexResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryAnswerCreateResult;
import com.back.catchmate.inquiry.presentation.dto.request.InquiryAnswerCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[관리자] 1:1 문의 API")
public interface AdminInquiryApiDocs {

    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 최신순으로 페이징 조회합니다.")
    ResponseEntity<OffsetPageResult<AdminInquiryResult>> getInquiries(
            @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    ResponseEntity<AdminInquiryDetailResult> getInquiry(Long inquiryId);

    @Operation(summary = "문의 답변 등록", description = "유저의 문의에 답변을 등록하고 상태를 '완료'로 변경합니다. (201)")
    ResponseEntity<InquiryAnswerCreateResult> createInquiryAnswer(
            Long inquiryId, @Valid InquiryAnswerCreateRequest request);

    @Operation(summary = "문의 답변 초안 생성(AI)", description = "공지·과거 답변을 근거로 RAG 답변 초안을 생성합니다. 근거가 없으면 직접 작성을 안내합니다.")
    ResponseEntity<AnswerDraftResult> createAnswerDraft(Long inquiryId);

    @Operation(summary = "RAG 코퍼스 수동 재색인", description = "공지·답변완료 문의를 벡터 스토어에 즉시 재적재합니다. (테스트/운영용)")
    ResponseEntity<CorpusReindexResult> reindexCorpus();
}
