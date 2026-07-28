package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.ChatNotificationUseCase;
import com.back.catchmate.notification.application.port.in.OutboxRecipient;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatNotificationService implements ChatNotificationUseCase {
    private static final String NOTIFICATION_TYPE = "CHAT";

    private final UserFetchPort userFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;

    @Override
    public void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<NotificationChatRecipientInfo> recipientsInfo = chatRoomFetchPort.getChatRoomRecipients(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        NotificationUserInfo sender = userFetchPort.getUser(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload = createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, NotificationChatRecipientInfo> infoMap = recipientsInfo.stream()
                .collect(Collectors.toMap(NotificationChatRecipientInfo::userId, Function.identity()));

        List<NotificationUserInfo> recipients = userFetchPort.getUsers(recipientsInfo.stream().map(NotificationChatRecipientInfo::userId).toList());

        // 알림 설정으로 먼저 걸러 Redis 조회 대상 자체를 줄인다.
        List<NotificationUserInfo> candidates = recipients.stream()
                // 해당 채팅방 알림이 꺼져있으면 아웃박스 저장 안함
                .filter(recipient -> infoMap.get(recipient.userId()).isNotificationOn())
                // 글로벌 채팅 알림이 꺼져있거나 토큰이 없으면 저장 안함
                .filter(recipient -> recipient.chatAlarmEnabled() && recipient.fcmToken() != null)
                .toList();

        // 수신자별 왕복 대신 MGET 한 번으로 포커스 방을 모아온다.
        Map<Long, Long> focusRooms = userOnlineStatusFetchPort.getUserFocusRooms(
                candidates.stream().map(NotificationUserInfo::userId).toList());

        // 현재 보고 있는 방이면 아웃박스 저장 안함 (FCM 발송 원천 방지)
        List<OutboxRecipient> outboxRecipients = candidates.stream()
                .filter(recipient -> !chatRoomId.equals(focusRooms.get(recipient.userId())))
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();

        // 필터 통과한 수신자 전원을 단일 멀티로우 INSERT 로 적재한다.
        outboxSaveUseCase.saveOutboxBatch(outboxRecipients, title, body, payload);
    }

    private static Map<String, String> createNotificationData(
            Long chatRoomId, Long senderId, String senderNickname, String content, String title, String body
    ) {
        return Map.of(
                "type", NOTIFICATION_TYPE,
                "roomId", chatRoomId.toString(),
                "senderId", senderId.toString(),
                "senderNickname", senderNickname,
                "content", content,
                "title", title,
                "body", body
        );
    }
}
