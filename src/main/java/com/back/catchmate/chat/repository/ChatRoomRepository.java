package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.entity.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    Optional<ChatRoom> findByBoardId(Long boardId);

    @Query("SELECT r FROM ChatRoom r " + "JOIN ChatRoomMember m ON r.id = m.chatRoom.id "
            + "WHERE m.userId = :userId AND m.leftAt IS NULL")
    Page<ChatRoom> findAllByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM ChatRoom r " + "JOIN ChatRoomMember m ON r.id = m.chatRoom.id "
            + "WHERE m.userId = :userId AND m.leftAt IS NULL")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT cr.lastMessageSequence FROM ChatRoom cr WHERE cr.id = :id AND cr.deletedAt IS NULL")
    Optional<Long> findLastMessageSequenceById(@Param("id") Long id);
}
