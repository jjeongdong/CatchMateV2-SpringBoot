package com.back.catchmate.api.inquiry.controller;

import com.back.catchmate.api.inquiry.dto.request.InquiryCreateRequest;
import com.back.catchmate.application.inquiry.InquiryUseCase;
import com.back.catchmate.application.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.application.inquiry.dto.response.InquiryDetailResponse;
import com.back.catchmate.domain.common.permission.PermissionId;
import com.back.catchmate.global.annotation.AuthUser;
import com.back.catchmate.global.aop.permission.CheckInquiryPermission;
import io.swagger.v3.oas.annotations.Operation;
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
    private final InquiryUseCase inquiryUseCase;

    @PostMapping
    @Operation(summary = "문의 등록", description = "새로운 1:1 문의를 등록합니다.")
    public ResponseEntity<InquiryCreateResponse> createInquiry(@AuthUser Long userId,
                                                               @RequestBody @Valid InquiryCreateRequest request) {
        return ResponseEntity.ok(inquiryUseCase.createInquiry(userId, request.toCommand()));
    }

    @CheckInquiryPermission
    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회", description = "문의 내용과 답변을 상세 조회합니다.")
    public ResponseEntity<InquiryDetailResponse> getInquiry(@AuthUser Long userId,
                                                            @PermissionId @PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryUseCase.getInquiry(userId, inquiryId));
    }
}
