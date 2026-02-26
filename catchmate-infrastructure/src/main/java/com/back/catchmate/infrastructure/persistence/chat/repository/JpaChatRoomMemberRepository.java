package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaChatRoomMemberRepository extends JpaRepository<ChatRoomMemberEntity, Long> {

    @Query("SELECT crm FROM ChatRoomMemberEntity crm " +
            "JOIN FETCH crm.chatRoom cr " +
            "JOIN FETCH crm.user u " +
            "WHERE crm.chatRoom.id = :chatRoomId " +
            "AND crm.user.id = :userId")
    Optional<ChatRoomMemberEntity> findByChatRoomIdAndUserId(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoomMemberEntity crm SET crm.lastReadSequence = :sequence " +
            "WHERE crm.chatRoom.id = :chatRoomId AND crm.user.id = :userId " +
            "AND crm.leftAt IS NULL " +
            "AND crm.lastReadSequence < :sequence") // 혹시 모를 역전 방지
    void updateLastReadSequenceDirectly(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId,
            @Param("sequence") Long sequence);

    @Query("SELECT crm FROM ChatRoomMemberEntity crm " +
            "JOIN FETCH crm.chatRoom cr " +
            "JOIN FETCH cr.board " +
            "WHERE crm.user.id = :userId " +
            "AND crm.leftAt IS NULL")
    List<ChatRoomMemberEntity> findAllByUserIdAndActive(@Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMemberEntity crm " +
            "JOIN FETCH crm.user u " +
            "WHERE crm.chatRoom.id = :chatRoomId " +
            "AND crm.leftAt IS NULL")
    List<ChatRoomMemberEntity> findAllByChatRoomIdAndActive(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(crm) > 0 FROM ChatRoomMemberEntity crm " +
            "WHERE crm.chatRoom.id = :chatRoomId " +
            "AND crm.user.id = :userId " +
            "AND crm.leftAt IS NULL")
    boolean existsByChatRoomIdAndUserIdAndActive(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId);
}
