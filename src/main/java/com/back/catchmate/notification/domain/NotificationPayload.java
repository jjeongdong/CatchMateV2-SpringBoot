package com.back.catchmate.notification.domain;

import java.util.Map;

// FCM data·STOMP 메시지 본문. 앱이 이 키로 화면을 열기 때문에 키·값 형식이 곧 앱과의 계약이다.
public final class NotificationPayload {
    public static final String TYPE = "type";
    public static final String ROOM_ID = "roomId";

    // 같은 알림의 재발송을 식별하는 키(값 = outbox 행 id). 수신 측 중복 제거와 FCM tag 로 쓴다.
    public static final String DEDUP_KEY = "dedupKey";

    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_ENROLL_REQUEST = "ENROLL_REQUEST";
    public static final String TYPE_ENROLL_ACCEPTED = "ENROLL_ACCEPTED";
    public static final String TYPE_ENROLL_REJECTED = "ENROLL_REJECTED";
    public static final String TYPE_ENROLL_CANCEL = "ENROLL_CANCEL";

    private static final String TYPE_INQUIRY = "INQUIRY";
    private static final String TYPE_NOTICE = "NOTICE";
    private static final String TITLE = "title";
    private static final String BODY = "body";

    private NotificationPayload() {}

    public static Map<String, String> enroll(String type, Long boardId, String title, String body) {
        return Map.of(TYPE, type, "boardId", String.valueOf(boardId), TITLE, title, BODY, body);
    }

    public static Map<String, String> chat(
            Long chatRoomId, Long senderId, String senderNickname, String content, String title, String body) {
        return Map.of(
                TYPE,
                TYPE_CHAT,
                ROOM_ID,
                chatRoomId.toString(),
                "senderId",
                senderId.toString(),
                "senderNickname",
                senderNickname,
                "content",
                content,
                TITLE,
                title,
                BODY,
                body);
    }

    // 옛 코드부터 문의·공지는 아웃박스(FCM)에 title/body 를 싣지 않았다 — FCM notification 필드에 이미 있다.
    public static Map<String, String> inquiryOutbox(Long inquiryId) {
        return Map.of(TYPE, TYPE_INQUIRY, "inquiryId", inquiryId.toString());
    }

    public static Map<String, String> inquiryRealtime(Long inquiryId, String title, String body) {
        return Map.of(TYPE, TYPE_INQUIRY, "inquiryId", inquiryId.toString(), TITLE, title, BODY, body);
    }

    public static Map<String, String> noticeOutbox(Long noticeId) {
        return Map.of(TYPE, TYPE_NOTICE, "noticeId", noticeId.toString());
    }

    public static Map<String, String> noticeRealtime(Long noticeId, String title, String body) {
        return Map.of(TYPE, TYPE_NOTICE, "noticeId", noticeId.toString(), TITLE, title, BODY, body);
    }

    public static boolean isChat(Map<String, String> payload) {
        return TYPE_CHAT.equals(payload.get(TYPE));
    }
}
