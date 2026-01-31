package com.back.catchmate.domain.enroll.repository;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public interface EnrollRepository {
    Enroll save(Enroll enroll);

    Optional<Enroll> findById(Long id);

    Optional<Enroll> findByIdWithLock(Long id);

    Optional<Enroll> findByIdWithFetch(Long id);

    Optional<Enroll> findByUserAndBoard(User user, Board board);

    DomainPage<Enroll> findAllByUserId(Long userId, DomainPageable pageable);

    DomainPage<Enroll> findAllByBoardIdAndStatus(Long boardId, AcceptStatus status, DomainPageable pageable);

    DomainPage<Long> findBoardIdsWithPendingEnrolls(Long userId, DomainPageable pageable);

    List<Enroll> findAllByBoardIds(List<Long> boardIds);

    long countByBoardWriterAndStatus(Long userId, AcceptStatus status);

    void delete(Enroll enroll);
}
