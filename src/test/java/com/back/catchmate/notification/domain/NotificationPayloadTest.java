package com.back.catchmate.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 앱이 FCM data·STOMP 메시지의 키로 화면을 연다. 키·값이 옛 서비스가 만들던 맵과 같아야 한다.
class NotificationPayloadTest {

    @Test
    @DisplayName("신청 알림 데이터")
    void enroll() {
        assertThat(NotificationPayload.enroll(NotificationPayload.TYPE_ENROLL_REQUEST, 10L, "제목", "본문"))
                .isEqualTo(Map.of("type", "ENROLL_REQUEST", "boardId", "10", "title", "제목", "body", "본문"));
    }

    @Test
    @DisplayName("채팅 알림 데이터")
    void chat() {
        assertThat(NotificationPayload.chat(50L, 1L, "철수", "안녕", "철수", "안녕"))
                .isEqualTo(Map.of(
                        "type", "CHAT",
                        "roomId", "50",
                        "senderId", "1",
                        "senderNickname", "철수",
                        "content", "안녕",
                        "title", "철수",
                        "body", "안녕"));
    }

    @Test
    @DisplayName("문의·공지는 아웃박스에 title/body 를 싣지 않고 실시간 메시지에만 싣는다")
    void inquiryAndNotice() {
        assertThat(NotificationPayload.inquiryOutbox(3L)).isEqualTo(Map.of("type", "INQUIRY", "inquiryId", "3"));
        assertThat(NotificationPayload.inquiryRealtime(3L, "t", "b"))
                .isEqualTo(Map.of("type", "INQUIRY", "inquiryId", "3", "title", "t", "body", "b"));
        assertThat(NotificationPayload.noticeOutbox(4L)).isEqualTo(Map.of("type", "NOTICE", "noticeId", "4"));
        assertThat(NotificationPayload.noticeRealtime(4L, "t", "b"))
                .isEqualTo(Map.of("type", "NOTICE", "noticeId", "4", "title", "t", "body", "b"));
    }

    @Test
    @DisplayName("채팅 알림 판별")
    void isChat() {
        assertThat(NotificationPayload.isChat(Map.of("type", "CHAT"))).isTrue();
        assertThat(NotificationPayload.isChat(Map.of("type", "NOTICE"))).isFalse();
        assertThat(NotificationPayload.isChat(Map.of())).isFalse();
    }
}
