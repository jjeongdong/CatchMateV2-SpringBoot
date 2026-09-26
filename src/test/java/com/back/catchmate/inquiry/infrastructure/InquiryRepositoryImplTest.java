package com.back.catchmate.inquiry.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.inquiry.domain.exception.InquiryNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryRepositoryImplTest {

    @Mock
    private InquiryJpaRepository inquiryJpaRepository;

    @InjectMocks
    private InquiryRepositoryImpl inquiryRepository;

    @Test
    @DisplayName("문의가 없으면 InquiryNotFoundException 을 던진다")
    void getByIdThrowsWhenMissing() {
        // given
        given(inquiryJpaRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryRepository.getById(99L)).isInstanceOf(InquiryNotFoundException.class);
    }
}
