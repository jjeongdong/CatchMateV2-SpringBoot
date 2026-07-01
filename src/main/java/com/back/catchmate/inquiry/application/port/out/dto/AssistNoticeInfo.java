package com.back.catchmate.inquiry.application.port.out.dto;

public record AssistNoticeInfo(
        Long noticeId,
        String title,
        String content
) {
}
