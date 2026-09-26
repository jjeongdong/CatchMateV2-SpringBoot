package com.back.catchmate.enroll.infrastructure;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollJpaRepository extends JpaRepository<Enroll, Long> {
    Optional<Enroll> findByUserIdAndBoardId(Long userId, Long boardId);

    long countByUserId(Long userId);

    long countByBoardIdAndAcceptStatus(Long boardId, AcceptStatus acceptStatus);

    long countByBoardOwnerIdAndAcceptStatus(Long boardOwnerId, AcceptStatus acceptStatus);

    @Query(
            "SELECT COUNT(DISTINCT e.boardId) FROM Enroll e WHERE e.boardOwnerId = :ownerId AND e.acceptStatus = :status")
    long countDistinctBoardIdsByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") AcceptStatus status);

    @Query(
            "SELECT e FROM Enroll e WHERE e.boardId IN :boardIds AND e.acceptStatus = :status ORDER BY e.createdAt DESC, e.id DESC")
    List<Enroll> findAllByBoardIdInAndStatus(
            @Param("boardIds") Collection<Long> boardIds, @Param("status") AcceptStatus status);

    @Query(
            "SELECT e FROM Enroll e WHERE e.userId = :applicantId AND e.boardOwnerId = :ownerId AND e.acceptStatus = :status")
    List<Enroll> findAllByApplicantIdAndBoardOwnerIdAndStatus(
            @Param("applicantId") Long applicantId,
            @Param("ownerId") Long ownerId,
            @Param("status") AcceptStatus status);

    @Query("SELECT e.id, e.acceptStatus FROM Enroll e WHERE e.id IN :ids")
    List<Object[]> findIdAndAcceptStatusByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT e.acceptStatus FROM Enroll e WHERE e.id = :id")
    Optional<AcceptStatus> findAcceptStatusById(@Param("id") Long id);
}
