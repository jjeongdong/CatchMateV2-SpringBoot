package com.back.catchmate.inquiry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryQueryApiTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryQueryApi inquiryQueryApi;

    @Test
    @DisplayName("답변 대기 문의 수를 센다")
    void countWaiting() {
        // given
        given(inquiryRepository.countByStatus(InquiryStatus.WAITING)).willReturn(3L);

        // when & then
        assertThat(inquiryQueryApi.countWaiting()).isEqualTo(3L);
    }
}
