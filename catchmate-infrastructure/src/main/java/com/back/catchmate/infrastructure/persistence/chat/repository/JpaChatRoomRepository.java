package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface
JpaChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("SELECT cr FROM ChatRoomEntity cr " +
            "JOIN FETCH cr.board b " +
            "WHERE cr.id = :id")
    Optional<ChatRoomEntity> findByIdWithBoard(@Param("id") Long id);

    @Query("SELECT cr FROM ChatRoomEntity cr " +
            "JOIN FETCH cr.board b " +
            "WHERE b.id = :boardId")
    Optional<ChatRoomEntity> findByBoardId(@Param("boardId") Long boardId);

    @Query("SELECT r FROM ChatRoomEntity r " +
            "JOIN ChatRoomMemberEntity m ON r.id = m.chatRoom.id " +
            "WHERE m.user.id = :userId AND m.leftAt IS NULL")
    Page<ChatRoomEntity> findAllByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM ChatRoomEntity r " +
            "JOIN ChatRoomMemberEntity m ON r.id = m.chatRoom.id " +
            "WHERE m.user.id = :userId AND m.leftAt IS NULL")
    List<ChatRoomEntity> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatRoomEntity c SET c.lastMessageSequence = :sequence " +
            "WHERE c.id = :roomId AND (c.lastMessageSequence IS NULL OR c.lastMessageSequence < :sequence)")
    void updateMaxSequence(@Param("roomId") Long roomId, @Param("sequence") Long sequence);
}
