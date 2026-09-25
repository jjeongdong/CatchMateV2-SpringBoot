package com.back.catchmate.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NoticeCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.") String title, @NotBlank(message = "내용을 입력해주세요.") String content) {}
