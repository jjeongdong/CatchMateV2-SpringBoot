package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.domain.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EnrollRepository {
    Enroll save(Enroll enroll);

    Optional<Enroll> findById(Long id);

    Optional<Enroll> findByIdWithFetch(Long id);

    Optional<Enroll> findByUserAndBoard(User user, Board board);

    DomainPage<Enroll> findAllByUserId(Long userId, DomainPageable pageable);

    DomainPage<Enroll> findAllByBoardIdAndStatus(Long boardId, AcceptStatus status, DomainPageable pageable);

    DomainPage<Long> findBoardIdsWithPendingEnrolls(Long userId, DomainPageable pageable);

    List<Enroll> findAllByBoardIds(List<Long> boardIds);

    List<Enroll> findAllByIds(List<Long> ids);

    Map<Long, AcceptStatus> findAcceptStatusMapByIds(List<Long> ids);

    Optional<AcceptStatus> findAcceptStatusById(Long id);

    long countByBoardWriterAndStatus(Long userId, AcceptStatus status);

    List<Enroll> findAllByApplicantAndBoardOwnerAndStatus(Long applicantId, Long ownerId, AcceptStatus status);

    void delete(Enroll enroll);
}
