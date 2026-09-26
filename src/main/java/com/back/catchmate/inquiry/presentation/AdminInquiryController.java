package com.back.catchmate.inquiry.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.InquiryAssistService;
import com.back.catchmate.inquiry.application.InquiryCommandService;
import com.back.catchmate.inquiry.application.InquiryQueryService;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryDetailResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryResult;
import com.back.catchmate.inquiry.application.dto.result.AnswerDraftResult;
import com.back.catchmate.inquiry.application.dto.result.CorpusReindexResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryAnswerCreateResult;
import com.back.catchmate.inquiry.presentation.dto.request.InquiryAnswerCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/inquiries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInquiryController implements AdminInquiryApiDocs {
    private final InquiryCommandService inquiryCommandService;
    private final InquiryQueryService inquiryQueryService;
    private final InquiryAssistService inquiryAssistService;

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<AdminInquiryResult>> getInquiries(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inquiryQueryService.getInquiries(page, size));
    }

    @Override
    @GetMapping("/{inquiryId}")
    public ResponseEntity<AdminInquiryDetailResult> getInquiry(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryQueryService.getInquiry(inquiryId));
    }

    @Override
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<InquiryAnswerCreateResult> createInquiryAnswer(
            @PathVariable Long inquiryId, @RequestBody InquiryAnswerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryCommandService.createInquiryAnswer(inquiryId, request.toCommand()));
    }

    @Override
    @PostMapping("/{inquiryId}/answer-draft")
    public ResponseEntity<AnswerDraftResult> createAnswerDraft(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryAssistService.draftAnswer(inquiryId));
    }

    @Override
    @PostMapping("/reindex")
    public ResponseEntity<CorpusReindexResult> reindexCorpus() {
        return ResponseEntity.ok(inquiryAssistService.reindexCorpus());
    }
}
