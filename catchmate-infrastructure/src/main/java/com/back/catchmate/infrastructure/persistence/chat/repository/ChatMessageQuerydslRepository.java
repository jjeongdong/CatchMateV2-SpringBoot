package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatMessageEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.back.catchmate.infrastructure.persistence.chat.entity.QChatMessageEntity.chatMessageEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

@Component
@RequiredArgsConstructor
public class ChatMessageQuerydslRepository {
    private final JPAQueryFactory queryFactory;

    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        return queryFactory
                .selectFrom(chatMessageEntity)
                .join(chatMessageEntity.sender, userEntity).fetchJoin()
                .where(
                        chatMessageEntity.chatRoom.id.eq(roomId),
                        ltMessageId(lastMessageId)
                )
                .orderBy(chatMessageEntity.id.desc())
                .limit(size)
                .fetch()
                .stream()
                .map(ChatMessageEntity::toModel)
                .toList();
    }

    private BooleanExpression ltMessageId(Long lastMessageId) {
        if (lastMessageId == null) {
            return null;
        }
        return chatMessageEntity.id.lt(lastMessageId);
    }
}
