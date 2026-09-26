package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatHistoryPage;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 같은 클래스 안 호출은 캐시 프록시를 타지 않아 별도 빈으로 둔다.
// 키 형식은 옛 캐시와 같고, ChatHistoryCache.evictLatestPage 의 "_START_" 패턴과 짝이다.
@Service
@RequiredArgsConstructor
public class ChatHistoryReader {
    private final ChatMessageRepository chatMessageRepository;
    private final UserQueryApi userQueryApi;

    @Cacheable(
            value = "chatHistory",
            key = "#chatRoomId + '_' + (#beforeMessageId != null ? #beforeMessageId : 'START') + '_' + #limit",
            cacheManager = "redisCacheManager")
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
