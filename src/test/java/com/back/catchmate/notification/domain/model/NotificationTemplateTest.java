package com.back.catchmate.notification.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationTemplateTest {

    @Test
    void ENROLL_REQUEST_formatTitle_사용자이름_바인딩() {
        String result = NotificationTemplate.ENROLL_REQUEST.formatTitle("홍길동");
        assertThat(result).isEqualTo("홍길동님이 참여 신청을 보냈습니다");
    }

    @Test
    void ENROLL_REQUEST_formatBody_게시글제목_바인딩() {
        String result = NotificationTemplate.ENROLL_REQUEST.formatBody("야구 직관 같이가요");
        assertThat(result).isEqualTo("'야구 직관 같이가요' 모임에 새로운 참여 신청이 도착했습니다.");
    }

    @Test
    void ENROLL_ACCEPT_formatBody_게시글제목_바인딩() {
        String result = NotificationTemplate.ENROLL_ACCEPT.formatBody("야구 직관 같이가요");
        assertThat(result).isEqualTo("축하합니다! '야구 직관 같이가요' 모임 참여가 수락되었습니다. 채팅방을 확인해보세요.");
    }

    @Test
    void ENROLL_REJECT_formatBody_게시글제목_바인딩() {
        String result = NotificationTemplate.ENROLL_REJECT.formatBody("야구 직관 같이가요");
        assertThat(result).isEqualTo("아쉽지만 '야구 직관 같이가요' 모임 참여 신청이 거절되었습니다.");
    }

    @Test
    void CHAT_NEW_MESSAGE_formatTitle_발신자이름_바인딩() {
        String result = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle("홍길동");
        assertThat(result).isEqualTo("홍길동");
    }

    @Test
    void CHAT_NEW_MESSAGE_formatBody_메시지내용_바인딩() {
        String result = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody("안녕하세요!");
        assertThat(result).isEqualTo("안녕하세요!");
    }

    @Test
    void INQUIRY_ANSWER_고정_제목_반환() {
        String result = NotificationTemplate.INQUIRY_ANSWER.formatTitle();
        assertThat(result).isEqualTo("1:1 문의 답변 완료");
    }

    @Test
    void INQUIRY_ANSWER_고정_본문_반환() {
        String result = NotificationTemplate.INQUIRY_ANSWER.formatBody();
        assertThat(result).isEqualTo("작성하신 1:1 문의에 답변이 등록되었습니다.");
    }

    @Test
    void 모든_템플릿_인수초과_예외없이_동작() {
        // varargs 특성상 초과 인수는 무시되므로 런타임 예외 없이 동작해야 함
        assertThatCode(() -> NotificationTemplate.ENROLL_ACCEPT.formatTitle("무시됨"))
                .doesNotThrowAnyException();
    }
}
