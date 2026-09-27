package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatHistoryPage;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatHistoryReader {
    private final ChatMessageRepository chatMessageRepository;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public ChatHistoryPage read(Long chatRoomId, Long beforeMessageId, int limit) {
        List<ChatMessage> messages = chatMessageRepository.findHistory(chatRoomId, beforeMessageId, limit);
        List<Long> senderIds =
                messages.stream().map(ChatMessage::getSenderId).distinct().toList();
        Map<Long, UserInfo> senderById = senderIds.isEmpty() ? Map.of() : userQueryApi.getInfos(senderIds);
        return new ChatHistoryPage(messages.stream()
                .map(message -> {
                    UserInfo sender = senderById.get(message.getSenderId());
                    return new ChatHistoryPage.Entry(
                            message.getId(),
                            message.getChatRoom().getId(),
                            message.getSenderId(),
                            sender != null ? sender.nickName() : null,
                            sender != null ? sender.profileImageUrl() : null,
                            message.getContent(),
                            message.getMessageType(),
                            message.getCreatedAt());
                })
                .toList());
    }
}
