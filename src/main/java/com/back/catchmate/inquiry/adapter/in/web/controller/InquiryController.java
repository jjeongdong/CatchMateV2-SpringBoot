package com.back.catchmate.inquiry.adapter.in.web.controller;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.inquiry.adapter.in.web.dto.request.InquiryCreateRequest;
import com.back.catchmate.inquiry.application.dto.response.InquiryCreateResponse;
import com.back.catchmate.inquiry.application.dto.response.InquiryDetailResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryClientCommandUseCase;
import com.back.catchmate.inquiry.application.port.in.InquiryClientQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 1:1 문의 API")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryClientCommandUseCase inquiryClientCommandUseCase;
    private final InquiryClientQueryUseCase inquiryClientQueryUseCase;

    @PostMapping
    @Operation(summary = "문의 등록", description = "새로운 1:1 문의를 등록합니다.")
    public ResponseEntity<InquiryCreateResponse> createInquiry(@AuthUser Long userId,
                                                               @RequestBody @Valid InquiryCreateRequest request) {
        return ResponseEntity.ok(inquiryClientCommandUseCase.createInquiry(userId, request.toCommand()));
    }

    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회", description = "문의 내용과 답변을 상세 조회합니다.")
    public ResponseEntity<InquiryDetailResponse> getInquiry(@AuthUser Long userId,
                                                            @PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryClientQueryUseCase.getInquiryDetail(userId, inquiryId));
    }

    @GetMapping
    @Operation(summary = "내 문의 목록 조회", description = "로그인한 사용자의 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<InquiryDetailResponse>> getInquiryList(@Parameter(hidden = true) @AuthUser Long userId,
                                                                               @RequestParam(defaultValue = "0") int page,
                                                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inquiryClientQueryUseCase.getInquiryListByUser(userId, page, size));
    }
}
