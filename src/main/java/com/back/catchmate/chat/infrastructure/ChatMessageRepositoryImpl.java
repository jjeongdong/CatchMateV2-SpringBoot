package com.back.catchmate.chat.infrastructure;

import static com.back.catchmate.chat.domain.QChatMessage.chatMessage;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.MessageType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public ChatMessage save(ChatMessage message) {
        return chatMessageJpaRepository.save(message);
    }

    @Override
    public List<ChatMessage> findHistory(Long chatRoomId, Long beforeMessageId, int limit) {
        // 인덱스(chat_room_id, deleted_at, chat_message_id DESC)로 ID 만 고른 뒤 본문을 읽는다 (옛 2단계 조회).
        List<Long> messageIds = jpaQueryFactory
                .select(chatMessage.id)
                .from(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(chatRoomId),
                        beforeMessageId != null ? chatMessage.id.lt(beforeMessageId) : null)
                .orderBy(chatMessage.id.desc())
                .limit(limit)
                .fetch();
        if (messageIds.isEmpty()) {
            return List.of();
        }
        return jpaQueryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.id.in(messageIds))
                .orderBy(chatMessage.id.asc())
                .fetch();
    }

    @Override
    public List<ChatMessage> findAfter(Long chatRoomId, Long afterMessageId, int limit) {
        return jpaQueryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(chatRoomId), after(afterMessageId))
                .orderBy(chatMessage.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<ChatMessage> findLastText(Long chatRoomId) {
        return chatMessageJpaRepository.findTopByChatRoomIdAndMessageTypeOrderByIdDesc(chatRoomId, MessageType.TEXT);
    }

    @Override
    public Map<Long, ChatMessage> findLastTextByChatRoomIds(Collection<Long> chatRoomIds) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }
        // 방별 마지막 TEXT 메시지 ID 를 그룹 집계로 먼저 구한다.
        List<Long> messageIds = jpaQueryFactory
                .select(chatMessage.id.max())
                .from(chatMessage)
                .where(chatMessage.chatRoom.id.in(chatRoomIds), chatMessage.messageType.eq(MessageType.TEXT))
                .groupBy(chatMessage.chatRoom.id)
                .fetch();
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return jpaQueryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.chatRoom)
                .fetchJoin()
                .where(chatMessage.id.in(messageIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(message -> message.getChatRoom().getId(), message -> message));
    }

    private BooleanExpression after(Long afterMessageId) {
        return afterMessageId != null ? chatMessage.id.gt(afterMessageId) : null;
    }
}
