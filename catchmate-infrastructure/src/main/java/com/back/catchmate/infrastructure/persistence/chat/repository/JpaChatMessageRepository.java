package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("SELECT cm FROM ChatMessageEntity cm " +
            "JOIN FETCH cm.sender s " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "ORDER BY cm.createdAt DESC")
    Page<ChatMessageEntity> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessageEntity cm " +
            "JOIN FETCH cm.sender s " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "ORDER BY cm.createdAt ASC")
    List<ChatMessageEntity> findAllByChatRoomIdList(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cm FROM ChatMessageEntity cm " +
            "JOIN FETCH cm.sender s " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "ORDER BY cm.createdAt DESC " +
            "LIMIT 1")
    Optional<ChatMessageEntity> findLastMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    Optional<ChatMessageEntity> findTopByChatRoomIdAndMessageTypeOrderByCreatedAtDesc(Long chatRoomId, MessageType messageType);
}
