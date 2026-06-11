package com.back.catchmate.api.inquiry.controller;

import com.back.catchmate.api.inquiry.dto.request.InquiryCreateRequest;
import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.authorization.annotation.CheckInquiryPermission;
import com.back.catchmate.authorization.annotation.PermissionId;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.inquiry.InquiryOrchestrator;
import com.back.catchmate.orchestration.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.orchestration.inquiry.dto.response.InquiryDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자] 1:1 문의 API")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryOrchestrator inquiryOrchestrator;

    @PostMapping
    @Operation(summary = "문의 등록", description = "새로운 1:1 문의를 등록합니다.")
    public ResponseEntity<InquiryCreateResponse> createInquiry(@AuthUser Long userId,
                                                               @RequestBody @Valid InquiryCreateRequest request) {
        return ResponseEntity.ok(inquiryOrchestrator.createInquiry(userId, request.toCommand()));
    }

    @CheckInquiryPermission
    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회", description = "문의 내용과 답변을 상세 조회합니다.")
    public ResponseEntity<InquiryDetailResponse> getInquiry(@AuthUser Long userId,
                                                            @PermissionId @PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryOrchestrator.getInquiry(inquiryId));
    }

    @GetMapping
    @Operation(summary = "내 문의 목록 조회", description = "로그인한 사용자의 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<InquiryDetailResponse>> getInquiryList(
            @Parameter(hidden = true) @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inquiryOrchestrator.getInquiryListByUser(userId, page, size));
    }
}
