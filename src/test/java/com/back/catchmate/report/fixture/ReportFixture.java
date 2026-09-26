package com.back.catchmate.report.fixture;

import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportReason;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class ReportFixture {

    public static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    private ReportFixture() {}

    public static Report report(Long reportId, Long reporterId, Long reportedUserId) {
        Report report = Report.create(reporterId, reportedUserId, ReportReason.SPAM, "도배");
        // 저장 없이 쓰는 단위 테스트용이라 id·생성 시각을 리플렉션으로 채운다.
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "createdAt", CREATED_AT);
        return report;
    }
}
