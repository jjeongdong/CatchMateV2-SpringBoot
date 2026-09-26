package com.back.catchmate.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.report.domain.exception.ReportSelfNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportTest {

    @Test
    @DisplayName("미처리 상태로 생성한다")
    void create() {
        // when
        Report report = Report.create(1L, 2L, ReportReason.SPAM, "도배");

        // then
        assertThat(report.getReporterId()).isEqualTo(1L);
        assertThat(report.getReportedUserId()).isEqualTo(2L);
        assertThat(report.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(report.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("자기 자신은 신고할 수 없다")
    void rejectsSelfReport() {
        assertThatThrownBy(() -> Report.create(1L, 1L, ReportReason.SPAM, null))
                .isInstanceOf(ReportSelfNotAllowedException.class);
    }

    @Test
    @DisplayName("처리하면 완료 상태가 된다")
    void process() {
        // given
        Report report = Report.create(1L, 2L, ReportReason.SPAM, null);

        // when
        report.process();

        // then
        assertThat(report.isCompleted()).isTrue();
    }
}
