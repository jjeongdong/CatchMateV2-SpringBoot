package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.board.domain.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
