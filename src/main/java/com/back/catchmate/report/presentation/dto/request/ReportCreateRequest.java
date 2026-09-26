package com.back.catchmate.report.presentation.dto.request;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.domain.ReportReason;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull(message = "신고할 유저 ID는 필수입니다.") Long reportedUserId,
        @NotNull(message = "신고 사유를 선택해주세요.") ReportReason reason,
        String description) {

    public ReportCreateCommand toCommand() {
        return new ReportCreateCommand(reportedUserId, reason, description);
    }
}
