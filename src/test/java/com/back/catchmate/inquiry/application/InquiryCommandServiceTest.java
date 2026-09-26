package com.back.catchmate.inquiry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.inquiry.application.dto.command.InquiryAnswerCreateCommand;
import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.result.InquiryAnswerCreateResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryCreateResult;
import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import com.back.catchmate.inquiry.domain.InquiryType;
import com.back.catchmate.inquiry.domain.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.inquiry.domain.exception.InquiryAlreadyAnsweredException;
import com.back.catchmate.inquiry.fixture.InquiryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InquiryCommandServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InquiryCommandService inquiryCommandService;

    @Test
    @DisplayName("문의를 등록한다")
    void createInquiry() {
        // given
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(InquiryFixture.waiting(5L, 1L));

        // when
        InquiryCreateResult result =
                inquiryCommandService.createInquiry(1L, new InquiryCreateCommand(InquiryType.ACCOUNT, "로그인이 안 돼요"));

        // then
        assertThat(result).isEqualTo(new InquiryCreateResult(5L, InquiryFixture.CREATED_AT));
    }

    @Test
    @DisplayName("답변을 등록하면 답변 완료로 바꾸고 작성자에게 알릴 이벤트를 발행한다")
    void publishesAnswerRegisteredEvent() {
        // given
        Inquiry inquiry = InquiryFixture.waiting(5L, 1L);
        given(inquiryRepository.getById(5L)).willReturn(inquiry);

        // when
        InquiryAnswerCreateResult result =
                inquiryCommandService.createInquiryAnswer(5L, new InquiryAnswerCreateCommand("재접속해 주세요"));

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(result.inquiryId()).isEqualTo(5L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.answeredAt()).isNotNull();
        then(eventPublisher).should().publishEvent(new InquiryAnswerRegisteredEvent(5L, 1L));
    }

    @Test
    @DisplayName("이미 답변한 문의면 이벤트를 발행하지 않는다")
    void doesNotPublishWhenAlreadyAnswered() {
        // given
        given(inquiryRepository.getById(5L)).willReturn(InquiryFixture.answered(5L, 1L, "기존 답변"));

        // when & then
        assertThatThrownBy(() -> inquiryCommandService.createInquiryAnswer(5L, new InquiryAnswerCreateCommand("다시")))
                .isInstanceOf(InquiryAlreadyAnsweredException.class);
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
