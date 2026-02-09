package com.back.catchmate.api.report.dto.request;

import com.back.catchmate.orchestration.report.dto.command.ReportCreateCommand;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.back.catchmate.report.enums.ReportReason;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {
    @NotNull(message = "신고할 유저 ID는 필수입니다.")
    private Long reportedUserId;

    @NotNull(message = "신고 사유를 선택해주세요.")
    private ReportReason reason;
    private String description;

    public ReportCreateCommand toCommand() {
        return ReportCreateCommand.builder()
                .reportedUserId(this.reportedUserId)
                .reason(this.reason)
                .description(this.description)
                .build();
    }
}
