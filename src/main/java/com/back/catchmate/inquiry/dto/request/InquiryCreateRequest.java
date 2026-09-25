package com.back.catchmate.inquiry.dto.request;

import com.back.catchmate.inquiry.entity.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InquiryCreateRequest(
        @NotNull(message = "문의 유형을 선택해주세요.") InquiryType type, @NotBlank(message = "내용을 입력해주세요.") String content) {}
