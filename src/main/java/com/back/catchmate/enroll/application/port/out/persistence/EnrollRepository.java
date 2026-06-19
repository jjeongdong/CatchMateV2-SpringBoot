package com.back.catchmate.enroll.application.port.out.persistence;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollRepository {
    Enroll save(Enroll enroll);

    Optional<Enroll> findById(Long id);

    Optional<Enroll> findByUserIdAndBoardId(Long userId, Long boardId);

    Page<Enroll> findAllByUserId(Long userId, Pageable pageable);

    Page<Enroll> findAllByBoardIdAndStatus(Long boardId, AcceptStatus status, Pageable pageable);

    Page<Long> findBoardIdsWithPendingEnrolls(Long userId, Pageable pageable);

    List<Enroll> findAllByBoardIds(List<Long> boardIds);

    List<Enroll> findAllByIds(List<Long> ids);

    Map<Long, AcceptStatus> findAcceptStatusMapByIds(List<Long> ids);

    Optional<AcceptStatus> findAcceptStatusById(Long id);

    long countByBoardWriterAndStatus(Long userId, AcceptStatus status);

    List<Enroll> findAllByApplicantAndBoardOwnerAndStatus(Long applicantId, Long ownerId, AcceptStatus status);

    void delete(Enroll enroll);
}
