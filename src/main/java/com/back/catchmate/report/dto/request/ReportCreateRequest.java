package com.back.catchmate.report.dto.request;

import com.back.catchmate.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull(message = "신고할 유저 ID는 필수입니다.") Long reportedUserId,
        @NotNull(message = "신고 사유를 선택해주세요.") ReportReason reason,
        String description
) {
}
