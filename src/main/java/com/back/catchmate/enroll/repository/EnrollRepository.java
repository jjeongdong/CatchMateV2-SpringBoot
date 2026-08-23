package com.back.catchmate.enroll.repository;

import com.back.catchmate.enroll.entity.AcceptStatus;
import com.back.catchmate.enroll.entity.Enroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollRepository extends JpaRepository<Enroll, Long> {
    Optional<Enroll> findByUserIdAndBoardId(Long userId, Long boardId);

    Page<Enroll> findAllByUserId(Long userId, Pageable pageable);

    Page<Enroll> findAllByBoardIdAndAcceptStatus(Long boardId, AcceptStatus acceptStatus, Pageable pageable);

    /**
     * 주어진 owner 가 보유한 게시글 중 PENDING 신청이 존재하는 boardId 들을 distinct 로 반환.
     * <p>정렬: 가장 최근 신청이 들어온 게시글이 위로 오도록 MAX(enroll.createdAt) 내림차순.
     * (이전엔 board.createdAt 기준이었으나, cross-context 조인 회피로 enroll 자체 컬럼 사용.)
     */
    @Query(value = "SELECT e.boardId FROM Enroll e " +
            "WHERE e.boardOwnerId = :ownerId " +
            "AND e.acceptStatus = :status " +
            "GROUP BY e.boardId " +
            "ORDER BY MAX(e.createdAt) DESC",
            countQuery = "SELECT COUNT(DISTINCT e.boardId) FROM Enroll e " +
                    "WHERE e.boardOwnerId = :ownerId " +
                    "AND e.acceptStatus = :status")
    Page<Long> findDistinctBoardIdsByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") AcceptStatus status,
            Pageable pageable
    );

    @Query("SELECT e FROM Enroll e " +
            "WHERE e.boardId IN :boardIds " +
            "AND e.acceptStatus = :status " +
            "ORDER BY e.createdAt DESC")
    List<Enroll> findAllByBoardIdInAndStatus(
            @Param("boardIds") List<Long> boardIds,
            @Param("status") AcceptStatus status
    );

    long countByBoardOwnerIdAndAcceptStatus(Long boardOwnerId, AcceptStatus acceptStatus);

    @Query("SELECT e FROM Enroll e " +
            "WHERE e.userId = :applicantId " +
            "AND e.boardOwnerId = :ownerId " +
            "AND e.acceptStatus = :status")
    List<Enroll> findAllByApplicantIdAndBoardOwnerIdAndStatus(
            @Param("applicantId") Long applicantId,
            @Param("ownerId") Long ownerId,
            @Param("status") AcceptStatus status
    );

    @Query("SELECT e.id, e.acceptStatus FROM Enroll e WHERE e.id IN :ids")
    List<Object[]> findIdAndAcceptStatusByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT e.acceptStatus FROM Enroll e WHERE e.id = :id")
    Optional<AcceptStatus> findAcceptStatusById(@Param("id") Long id);
}
