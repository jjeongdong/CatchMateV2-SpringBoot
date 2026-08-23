package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.chat.service.ChatQueryService;
import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;
import com.back.catchmate.notification.application.port.out.external.ChatRoomFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationChatRoomFetchAdapter implements ChatRoomFetchPort {
    private final ChatQueryService chatQueryService;

    @Override
    public List<NotificationChatRecipientInfo> getChatRoomRecipients(Long chatRoomId, Long excludeUserId) {
        return chatQueryService.getChatRoomRecipientSummaries(chatRoomId, excludeUserId).stream()
                .map(info -> new NotificationChatRecipientInfo(
                        info.userId(),
                        info.isNotificationOn()
                ))
                .toList();
    }
}
