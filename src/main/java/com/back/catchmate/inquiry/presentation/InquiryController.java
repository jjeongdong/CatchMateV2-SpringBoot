package com.back.catchmate.inquiry.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.InquiryCommandService;
import com.back.catchmate.inquiry.application.InquiryQueryService;
import com.back.catchmate.inquiry.application.dto.result.InquiryCreateResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryResult;
import com.back.catchmate.inquiry.presentation.dto.request.InquiryCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController implements InquiryApiDocs {
    private final InquiryCommandService inquiryCommandService;
    private final InquiryQueryService inquiryQueryService;

    @Override
    @PostMapping
    public ResponseEntity<InquiryCreateResult> createInquiry(
            @AuthUser Long userId, @RequestBody InquiryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryCommandService.createInquiry(userId, request.toCommand()));
    }

    @Override
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryResult> getMyInquiry(@AuthUser Long userId, @PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryQueryService.getMyInquiry(userId, inquiryId));
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<InquiryResult>> getMyInquiries(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inquiryQueryService.getMyInquiries(userId, page, size));
    }
}
