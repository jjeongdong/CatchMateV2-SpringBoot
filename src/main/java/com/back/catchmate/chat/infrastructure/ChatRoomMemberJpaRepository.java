package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatRoomMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMember, Long> {
    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId AND crm.userId = :userId")
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(
            @Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId AND crm.leftAt IS NULL")
    List<ChatRoomMember> findActiveByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.chatRoom cr "
            + "WHERE crm.chatRoom.id IN :chatRoomIds AND crm.userId = :userId AND crm.leftAt IS NULL")
    List<ChatRoomMember> findActiveByChatRoomIdsAndUserId(
            @Param("chatRoomIds") Collection<Long> chatRoomIds, @Param("userId") Long userId);
}
