package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.entity.ChatRoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long>, ChatRoomMemberRepositoryCustom {

    @Query("SELECT crm FROM ChatRoomMember crm " + "WHERE crm.chatRoom.id = :chatRoomId " + "AND crm.userId = :userId")
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(
            @Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm " + "JOIN FETCH crm.chatRoom cr "
            + "WHERE crm.userId = :userId "
            + "AND crm.leftAt IS NULL")
    List<ChatRoomMember> findAllByUserIdAndActive(@Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm " + "WHERE crm.chatRoom.id = :chatRoomId " + "AND crm.leftAt IS NULL")
    List<ChatRoomMember> findAllByChatRoomIdAndActive(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(crm) > 0 FROM ChatRoomMember crm " + "WHERE crm.chatRoom.id = :chatRoomId "
            + "AND crm.userId = :userId "
            + "AND crm.leftAt IS NULL")
    boolean existsByChatRoomIdAndUserIdAndActive(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm " + "JOIN FETCH crm.chatRoom cr "
            + "WHERE crm.chatRoom.id IN :chatRoomIds "
            + "AND crm.userId = :userId "
            + "AND crm.leftAt IS NULL")
    List<ChatRoomMember> findByChatRoomIdsAndUserId(
            @Param("chatRoomIds") List<Long> chatRoomIds, @Param("userId") Long userId);
}
