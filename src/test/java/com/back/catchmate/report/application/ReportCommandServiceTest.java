package com.back.catchmate.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.result.ReportCreateResult;
import com.back.catchmate.report.application.dto.result.ReportProcessResult;
import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportReason;
import com.back.catchmate.report.domain.ReportRepository;
import com.back.catchmate.report.domain.event.ReportProcessedEvent;
import com.back.catchmate.report.fixture.ReportFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportCommandService reportCommandService;

    @Test
    @DisplayName("신고를 접수한다")
    void createReport() {
        // given
        given(reportRepository.save(any(Report.class))).willReturn(ReportFixture.report(5L, 1L, 2L));

        // when
        ReportCreateResult result =
                reportCommandService.createReport(1L, new ReportCreateCommand(2L, ReportReason.SPAM, "도배"));

        // then
        assertThat(result).isEqualTo(new ReportCreateResult(5L, ReportFixture.CREATED_AT));
    }

    @Test
    @DisplayName("신고를 처리하면 완료로 바꾸고 ReportProcessedEvent 를 발행한다")
    void processPublishesEvent() {
        // given
        Report report = ReportFixture.report(5L, 1L, 2L);
        given(reportRepository.getById(5L)).willReturn(report);

        // when
        ReportProcessResult result = reportCommandService.processReport(5L);

        // then
        assertThat(report.isCompleted()).isTrue();
        assertThat(result).isEqualTo(new ReportProcessResult(5L, 2L));
        then(eventPublisher).should().publishEvent(new ReportProcessedEvent(5L, 2L));
    }
}
