package com.back.catchmate.chat.adapter.out.persistence.repository;

import com.back.catchmate.chat.adapter.out.persistence.entity.ChatRoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    Optional<ChatRoomEntity> findByBoardId(Long boardId);

    @Query("SELECT r FROM ChatRoomEntity r " +
            "JOIN ChatRoomMemberEntity m ON r.id = m.chatRoom.id " +
            "WHERE m.userId = :userId AND m.leftAt IS NULL")
    Page<ChatRoomEntity> findAllByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM ChatRoomEntity r " +
            "JOIN ChatRoomMemberEntity m ON r.id = m.chatRoom.id " +
            "WHERE m.userId = :userId AND m.leftAt IS NULL")
    List<ChatRoomEntity> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatRoomEntity c SET c.lastMessageSequence = :sequence " +
            "WHERE c.id = :roomId AND (c.lastMessageSequence IS NULL OR c.lastMessageSequence < :sequence)")
    void updateMaxSequence(@Param("roomId") Long roomId, @Param("sequence") Long sequence);

    @Query("SELECT cr.lastMessageSequence FROM ChatRoomEntity cr WHERE cr.id = :id AND cr.deletedAt IS NULL")
    Optional<Long> findLastMessageSequenceById(@Param("id") Long id);
}
