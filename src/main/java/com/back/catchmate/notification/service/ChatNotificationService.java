package com.back.catchmate.notification.service;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.notification.dto.OutboxRecipient;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatNotificationService {
    private static final String NOTIFICATION_TYPE = "CHAT";

    private final UserQueryApi userQueryApi;
    private final ChatQueryApi chatQueryApi;
    private final OutboxSaver outboxSaver;

    public void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<ChatRecipientInfo> recipientsInfo = chatQueryApi.getRecipients(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        UserInfo sender = userQueryApi.getInfo(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload =
                createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, ChatRecipientInfo> infoMap =
                recipientsInfo.stream().collect(Collectors.toMap(ChatRecipientInfo::userId, Function.identity()));

        List<UserInfo> recipients = List.copyOf(userQueryApi
                .getInfos(recipientsInfo.stream().map(ChatRecipientInfo::userId).toList())
                .values());

        // 알림 설정으로 먼저 걸러 Redis 조회 대상 자체를 줄인다.
        List<UserInfo> candidates = recipients.stream()
                // 해당 채팅방 알림이 꺼져있으면 아웃박스 저장 안함
                .filter(recipient -> infoMap.get(recipient.userId()).isNotificationOn())
                // 글로벌 채팅 알림이 꺼져있거나 토큰이 없으면 저장 안함
                .filter(recipient -> recipient.chatAlarmEnabled() && recipient.fcmToken() != null)
                .toList();

        // 수신자별 왕복 대신 MGET 한 번으로 포커스 방을 모아온다.
        Map<Long, Long> focusRooms = chatQueryApi.getFocusRooms(
                candidates.stream().map(UserInfo::userId).toList());

        // 현재 보고 있는 방이면 아웃박스 저장 안함 (FCM 발송 원천 방지)
        List<OutboxRecipient> outboxRecipients = candidates.stream()
                .filter(recipient -> !chatRoomId.equals(focusRooms.get(recipient.userId())))
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();

        // 필터 통과한 수신자 전원을 단일 멀티로우 INSERT 로 적재한다.
        outboxSaver.saveOutboxBatch(outboxRecipients, title, body, payload);
    }

    private static Map<String, String> createNotificationData(
            Long chatRoomId, Long senderId, String senderNickname, String content, String title, String body) {
        return Map.of(
                "type", NOTIFICATION_TYPE,
                "roomId", chatRoomId.toString(),
                "senderId", senderId.toString(),
                "senderNickname", senderNickname,
                "content", content,
                "title", title,
                "body", body);
    }
}
