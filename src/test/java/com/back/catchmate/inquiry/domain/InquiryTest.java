package com.back.catchmate.inquiry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.inquiry.domain.exception.InquiryAlreadyAnsweredException;
import com.back.catchmate.inquiry.domain.exception.InquiryNotOwnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InquiryTest {

    @Test
    @DisplayName("문의는 답변 대기 상태로 생성된다")
    void createsWaiting() {
        // when
        Inquiry inquiry = Inquiry.create(1L, InquiryType.CHAT, "채팅이 안 와요");

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.WAITING);
        assertThat(inquiry.getAnswer()).isNull();
        assertThat(inquiry.getUserId()).isEqualTo(1L);
    }

    @Nested
    @DisplayName("답변 등록")
    class RegisterAnswer {

        @Test
        @DisplayName("답변을 저장하고 답변 완료로 바꾼다")
        void marksAnswered() {
            // given
            Inquiry inquiry = Inquiry.create(1L, InquiryType.CHAT, "채팅이 안 와요");

            // when
            inquiry.registerAnswer("재접속해 주세요");

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
            assertThat(inquiry.getAnswer()).isEqualTo("재접속해 주세요");
        }

        @Test
        @DisplayName("이미 답변한 문의에는 다시 답변할 수 없다")
        void throwsWhenAlreadyAnswered() {
            // given
            Inquiry inquiry = Inquiry.create(1L, InquiryType.CHAT, "채팅이 안 와요");
            inquiry.registerAnswer("재접속해 주세요");

            // when & then
            assertThatThrownBy(() -> inquiry.registerAnswer("다시")).isInstanceOf(InquiryAlreadyAnsweredException.class);
        }
    }

    @Nested
    @DisplayName("작성자 확인")
    class ValidateOwner {

        @Test
        @DisplayName("작성자면 통과한다")
        void passesForOwner() {
            // given
            Inquiry inquiry = Inquiry.create(1L, InquiryType.CHAT, "채팅이 안 와요");

            // when & then
            assertThatCode(() -> inquiry.validateOwner(1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("작성자가 아니면 InquiryNotOwnerException")
        void throwsForOtherUser() {
            // given
            Inquiry inquiry = Inquiry.create(1L, InquiryType.CHAT, "채팅이 안 와요");

            // when & then
            assertThatThrownBy(() -> inquiry.validateOwner(2L)).isInstanceOf(InquiryNotOwnerException.class);
        }
    }
}
