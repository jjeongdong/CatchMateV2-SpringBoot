package com.back.catchmate.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.report.domain.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportQueryApiTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportQueryApi reportQueryApi;

    @Test
    @DisplayName("전체 신고 수와 미처리 신고 수를 센다")
    void counts() {
        // given
        given(reportRepository.count()).willReturn(7L);
        given(reportRepository.countByCompleted(false)).willReturn(3L);

        // when & then
        assertThat(reportQueryApi.count()).isEqualTo(7L);
        assertThat(reportQueryApi.countPending()).isEqualTo(3L);
    }
}
