package com.back.catchmate.notification.service;

import com.back.catchmate.chat.dto.response.ChatRecipientSummary;
import com.back.catchmate.chat.service.ChatQueryService;
import com.back.catchmate.notification.dto.OutboxRecipient;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserOnlineStatusService;
import com.back.catchmate.user.service.UserService;
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

    private final UserService userService;
    private final ChatQueryService chatQueryService;
    private final UserOnlineStatusService userOnlineStatusService;
    private final OutboxSaver outboxSaver;

    public void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<ChatRecipientSummary> recipientsInfo =
                chatQueryService.getChatRoomRecipientSummaries(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        UserSummary sender = userService.getUserSummary(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload =
                createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, ChatRecipientSummary> infoMap =
                recipientsInfo.stream().collect(Collectors.toMap(ChatRecipientSummary::userId, Function.identity()));

        List<UserSummary> recipients = userService.getUserSummaries(
                recipientsInfo.stream().map(ChatRecipientSummary::userId).toList());

        // 알림 설정으로 먼저 걸러 Redis 조회 대상 자체를 줄인다.
        List<UserSummary> candidates = recipients.stream()
                // 해당 채팅방 알림이 꺼져있으면 아웃박스 저장 안함
                .filter(recipient -> infoMap.get(recipient.userId()).isNotificationOn())
                // 글로벌 채팅 알림이 꺼져있거나 토큰이 없으면 저장 안함
                .filter(recipient -> recipient.chatAlarmEnabled() && recipient.fcmToken() != null)
                .toList();

        // 수신자별 왕복 대신 MGET 한 번으로 포커스 방을 모아온다.
        Map<Long, Long> focusRooms = userOnlineStatusService.getUserFocusRooms(
                candidates.stream().map(UserSummary::userId).toList());

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
