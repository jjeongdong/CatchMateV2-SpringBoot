package com.back.catchmate.infrastructure.persistence.enroll.repository;

import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.infrastructure.persistence.enroll.entity.EnrollEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaEnrollRepository extends JpaRepository<EnrollEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EnrollEntity e WHERE e.id = :id")
    Optional<EnrollEntity> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT e FROM EnrollEntity e " +
            "JOIN FETCH e.user u " +
            "JOIN FETCH e.board b " +
            "JOIN FETCH b.user bu " +
            "WHERE e.id = :enrollId")
    Optional<EnrollEntity> findByIdWithFetch(@Param("enrollId") Long enrollId);

    Optional<EnrollEntity> findByUserIdAndBoardId(Long userId, Long boardId);

    @Query("SELECT e FROM EnrollEntity e " +
            "JOIN FETCH e.board b " +
            "JOIN FETCH b.user " +
            "JOIN FETCH b.cheerClub " +
            "JOIN FETCH b.game " +
            "WHERE e.user.id = :userId")
    Page<EnrollEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e FROM EnrollEntity e " +
            "JOIN FETCH e.user " +
            "WHERE e.board.id = :boardId " +
            "AND e.acceptStatus = :acceptStatus")
    Page<EnrollEntity> findAllByBoardId(@Param("boardId") Long boardId,
                                        @Param("acceptStatus") AcceptStatus acceptStatus,
                                        Pageable pageable);

    @Query(value = "SELECT e.board.id FROM EnrollEntity e " +
            "WHERE e.board.user.id = :userId " +
            "AND e.acceptStatus = :status " +
            "GROUP BY e.board.id " +
            "ORDER BY e.board.createdAt DESC",
            countQuery = "SELECT count(DISTINCT e.board.id) FROM EnrollEntity e " +
                    "WHERE e.board.user.id = :userId " +
                    "AND e.acceptStatus = :status")
    Page<Long> findDistinctBoardIdsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") AcceptStatus status,
            Pageable pageable
    );

    @Query("SELECT e FROM EnrollEntity e " +
            "JOIN FETCH e.board b " +
            "JOIN FETCH e.user u " +
            "WHERE b.id IN :boardIds " +
            "AND e.acceptStatus = :status " +
            "ORDER BY e.createdAt DESC")
    List<EnrollEntity> findAllByBoardIdInAndStatus(
            @Param("boardIds") List<Long> boardIds,
            @Param("status") AcceptStatus status
    );

    long countByBoardUserIdAndAcceptStatus(Long userId, AcceptStatus acceptStatus);
}
