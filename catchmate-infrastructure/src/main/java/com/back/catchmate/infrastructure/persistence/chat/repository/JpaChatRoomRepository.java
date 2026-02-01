package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("SELECT cr FROM ChatRoomEntity cr " +
            "JOIN FETCH cr.board b " +
            "WHERE cr.id = :id")
    Optional<ChatRoomEntity> findByIdWithBoard(@Param("id") Long id);

    @Query("SELECT cr FROM ChatRoomEntity cr " +
            "JOIN FETCH cr.board b " +
            "WHERE b.id = :boardId")
    Optional<ChatRoomEntity> findByBoardId(@Param("boardId") Long boardId);

    @Query("SELECT cr FROM ChatRoomEntity cr " +
            "JOIN FETCH cr.board b " +
            "WHERE cr.id IN :chatRoomIds")
    Page<ChatRoomEntity> findAllByIdIn(@Param("chatRoomIds") List<Long> chatRoomIds, Pageable pageable);

    boolean existsByBoardId(Long boardId);
}
