package com.back.catchmate.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeCreateResponse(Long noticeId, LocalDateTime createdAt) {}
