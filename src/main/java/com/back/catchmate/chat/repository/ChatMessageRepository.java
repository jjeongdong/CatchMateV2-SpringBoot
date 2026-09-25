package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.MessageType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    @Query("SELECT cm FROM ChatMessage cm " + "WHERE cm.chatRoom.id = :chatRoomId " + "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm " + "WHERE cm.chatRoom.id = :chatRoomId " + "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findAllByChatRoomIdList(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cm FROM ChatMessage cm " + "WHERE cm.chatRoom.id = :chatRoomId "
            + "ORDER BY cm.createdAt DESC "
            + "LIMIT 1")
    Optional<ChatMessage> findLastMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    Optional<ChatMessage> findTopByChatRoomIdAndMessageTypeOrderByIdDesc(Long chatRoomId, MessageType messageType);
}
