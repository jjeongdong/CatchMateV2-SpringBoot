package com.back.catchmate.enroll.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollRepository {

    Enroll save(Enroll enroll);

    void delete(Enroll enroll);

    // 없으면 EnrollNotFoundException
    Enroll getById(Long enrollId);

    Optional<Enroll> findByApplicantIdAndBoardId(Long applicantId, Long boardId);

    // 최신 신청순 (등록 시각 내림차순 → id 내림차순)
    List<Enroll> findAllByApplicantId(Long applicantId, long offset, int limit);

    long countByApplicantId(Long applicantId);

    // 게시글의 대기 중 신청, 최신순
    List<Enroll> findPendingByBoardId(Long boardId, long offset, int limit);

    long countPendingByBoardId(Long boardId);

    // 작성자의 게시글 중 대기 신청이 있는 게시글 ID, 가장 최근 신청이 들어온 게시글부터
    List<Long> findBoardIdsWithPendingByOwnerId(Long ownerId, long offset, int limit);

    long countBoardsWithPendingByOwnerId(Long ownerId);

    // 여러 게시글의 대기 중 신청, 최신순
    List<Enroll> findPendingByBoardIds(Collection<Long> boardIds);

    long countPendingByOwnerId(Long ownerId);

    List<Enroll> findAcceptedByApplicantIdAndOwnerId(Long applicantId, Long ownerId);

    Map<Long, AcceptStatus> findAcceptStatusesByIds(Collection<Long> enrollIds);

    Optional<AcceptStatus> findAcceptStatusById(Long enrollId);
}
