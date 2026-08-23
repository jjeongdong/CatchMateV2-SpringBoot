package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.MessageType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.back.catchmate.chat.entity.QChatMessage.chatMessage;

@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        List<Long> messageIds = jpaQueryFactory
                .select(chatMessage.id)
                .from(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        lastMessageId != null ? chatMessage.id.lt(lastMessageId) : null
                )
                .orderBy(chatMessage.id.desc())
                .limit(size)
                .fetch();

        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.id.in(messageIds))
                .orderBy(chatMessage.id.asc())
                .fetch();
    }

    @Override
    public List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size) {
        return jpaQueryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        gtMessageId(lastMessageId)
                )
                .orderBy(chatMessage.id.asc())
                .limit(size)
                .fetch();
    }

    @Override
    public Map<Long, ChatMessage> findLastTextMessagesByChatRoomIds(List<Long> chatRoomIds) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }

        // 각 채팅방별 마지막 TEXT 메시지 ID를 서브쿼리로 조회
        List<Long> messageIds = jpaQueryFactory
                .select(chatMessage.id.max())
                .from(chatMessage)
                .where(
                        chatMessage.chatRoom.id.in(chatRoomIds),
                        chatMessage.messageType.eq(MessageType.TEXT)
                )
                .groupBy(chatMessage.chatRoom.id)
                .fetch();

        if (messageIds.isEmpty()) {
            return Map.of();
        }

        List<ChatMessage> messages = jpaQueryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.chatRoom).fetchJoin()
                .where(chatMessage.id.in(messageIds))
                .fetch();

        return messages.stream()
                .collect(Collectors.toMap(
                        msg -> msg.getChatRoom().getId(),
                        msg -> msg
                ));
    }

    private BooleanExpression gtMessageId(Long lastMessageId) {
        if (lastMessageId == null) {
            return null;
        }
        return chatMessage.id.gt(lastMessageId);
    }
}
