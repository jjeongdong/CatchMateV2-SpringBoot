package com.back.catchmate.report.adapter.in.web.dto.request;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import jakarta.validation.constraints.NotNull;
import com.back.catchmate.report.domain.enums.ReportReason;

public record ReportCreateRequest(
        @NotNull(message = "신고할 유저 ID는 필수입니다.") Long reportedUserId,
        @NotNull(message = "신고 사유를 선택해주세요.") ReportReason reason,
        String description
) {
    public ReportCreateCommand toCommand() {
        return ReportCreateCommand.builder()
                .reportedUserId(this.reportedUserId)
                .reason(this.reason)
                .description(this.description)
                .build();
    }
}
