package com.back.catchmate.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.report.domain.exception.ReportNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportRepositoryImplTest {

    @Mock
    private ReportJpaRepository reportJpaRepository;

    @InjectMocks
    private ReportRepositoryImpl reportRepository;

    @Test
    @DisplayName("신고가 없으면 ReportNotFoundException 을 던진다")
    void getByIdThrowsWhenMissing() {
        // given
        given(reportJpaRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportRepository.getById(99L)).isInstanceOf(ReportNotFoundException.class);
    }
}
