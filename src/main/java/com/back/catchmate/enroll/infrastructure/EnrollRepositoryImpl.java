package com.back.catchmate.enroll.infrastructure;

import static com.back.catchmate.enroll.domain.QEnroll.enroll;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.exception.EnrollNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollRepositoryImpl implements EnrollRepository {
    private final EnrollJpaRepository enrollJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Enroll save(Enroll newEnroll) {
        return enrollJpaRepository.save(newEnroll);
    }

    @Override
    public void delete(Enroll target) {
        enrollJpaRepository.delete(target);
    }

    @Override
    public Enroll getById(Long enrollId) {
        return enrollJpaRepository.findById(enrollId).orElseThrow(EnrollNotFoundException::new);
    }

    @Override
    public Optional<Enroll> findByApplicantIdAndBoardId(Long applicantId, Long boardId) {
        return enrollJpaRepository.findByUserIdAndBoardId(applicantId, boardId);
    }

    @Override
    public List<Enroll> findAllByApplicantId(Long applicantId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(enroll)
                .where(enroll.userId.eq(applicantId))
                .orderBy(enroll.createdAt.desc(), enroll.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByApplicantId(Long applicantId) {
        return enrollJpaRepository.countByUserId(applicantId);
    }

    @Override
    public List<Enroll> findPendingByBoardId(Long boardId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(enroll)
                .where(enroll.boardId.eq(boardId), enroll.acceptStatus.eq(AcceptStatus.PENDING))
                .orderBy(enroll.createdAt.desc(), enroll.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countPendingByBoardId(Long boardId) {
        return enrollJpaRepository.countByBoardIdAndAcceptStatus(boardId, AcceptStatus.PENDING);
    }

    @Override
    public List<Long> findBoardIdsWithPendingByOwnerId(Long ownerId, long offset, int limit) {
        // board 테이블을 JOIN 하지 않으려고 게시글 생성 시각 대신 가장 최근 신청 시각으로 정렬한다 (옛 동작).
        return jpaQueryFactory
                .select(enroll.boardId)
                .from(enroll)
                .where(enroll.boardOwnerId.eq(ownerId), enroll.acceptStatus.eq(AcceptStatus.PENDING))
                .groupBy(enroll.boardId)
                .orderBy(enroll.createdAt.max().desc(), enroll.boardId.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countBoardsWithPendingByOwnerId(Long ownerId) {
        return enrollJpaRepository.countDistinctBoardIdsByOwnerIdAndStatus(ownerId, AcceptStatus.PENDING);
    }

    @Override
    public List<Enroll> findPendingByBoardIds(Collection<Long> boardIds) {
        return enrollJpaRepository.findAllByBoardIdInAndStatus(boardIds, AcceptStatus.PENDING);
    }

    @Override
    public long countPendingByOwnerId(Long ownerId) {
        return enrollJpaRepository.countByBoardOwnerIdAndAcceptStatus(ownerId, AcceptStatus.PENDING);
    }

    @Override
    public List<Enroll> findAcceptedByApplicantIdAndOwnerId(Long applicantId, Long ownerId) {
        return enrollJpaRepository.findAllByApplicantIdAndBoardOwnerIdAndStatus(
                applicantId, ownerId, AcceptStatus.ACCEPTED);
    }

    @Override
    public Map<Long, AcceptStatus> findAcceptStatusesByIds(Collection<Long> enrollIds) {
        return enrollJpaRepository.findIdAndAcceptStatusByIdIn(enrollIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (AcceptStatus) row[1]));
    }

    @Override
    public Optional<AcceptStatus> findAcceptStatusById(Long enrollId) {
        return enrollJpaRepository.findAcceptStatusById(enrollId);
    }
}
