package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatMessageEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.back.catchmate.infrastructure.persistence.chat.entity.QChatMessageEntity.chatMessageEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

@Component
@RequiredArgsConstructor
public class QueryDSLChatMessageRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        List<Long> messageIds = jpaQueryFactory
                .select(chatMessageEntity.id)
                .from(chatMessageEntity)
                .where(
                        chatMessageEntity.chatRoom.id.eq(roomId),
                        lastMessageId != null ? chatMessageEntity.id.lt(lastMessageId) : null
                )
                .orderBy(chatMessageEntity.id.desc())
                .limit(size)
                .fetch();

        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessageEntity> entities = jpaQueryFactory
                .selectFrom(chatMessageEntity)
                .join(chatMessageEntity.sender, userEntity).fetchJoin()
                .where(chatMessageEntity.id.in(messageIds))
                .orderBy(chatMessageEntity.id.desc())
                .fetch();

        return entities.stream()
                .map(ChatMessageEntity::toModel)
                .toList();
    }

    private BooleanExpression ltMessageId(Long lastMessageId) {
        if (lastMessageId == null) {
            return null;
        }
        return chatMessageEntity.id.lt(lastMessageId);
    }

    public List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size) {
        return jpaQueryFactory
                .selectFrom(chatMessageEntity)
                .join(chatMessageEntity.sender, userEntity).fetchJoin()
                .where(
                        chatMessageEntity.chatRoom.id.eq(roomId),
                        gtMessageId(lastMessageId)
                )
                .orderBy(chatMessageEntity.id.asc())
                .limit(size)
                .fetch()
                .stream()
                .map(ChatMessageEntity::toModel)
                .toList();
    }

    private BooleanExpression gtMessageId(Long lastMessageId) {
        if (lastMessageId == null) {
            return null;
        }
        return chatMessageEntity.id.gt(lastMessageId);
    }
}
