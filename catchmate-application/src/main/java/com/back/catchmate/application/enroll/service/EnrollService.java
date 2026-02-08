package com.back.catchmate.application.enroll.service;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.repository.EnrollRepository;
import com.back.catchmate.domain.user.model.User;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnrollService {
    private final EnrollRepository enrollRepository;

    public Enroll createEnroll(User user, Board board, String description) {
        validateDuplicateEnroll(user, board);
        Enroll enroll = Enroll.createEnroll(user, board, description);
        return enrollRepository.save(enroll);
    }

    public Enroll getEnroll(Long enrollId) {
        return enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Enroll getEnrollWithLock(Long enrollId) {
        return enrollRepository.findByIdWithLock(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Enroll getEnrollWithFetch(Long enrollId) {
        return enrollRepository.findByIdWithFetch(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Optional<Enroll> findEnrollByUserAndBoard(User user, Board board) {
        return enrollRepository.findByUserAndBoard(user, board);
    }

    public DomainPage<Enroll> getEnrollListByUserId(Long userId, DomainPageable pageable) {
        return enrollRepository.findAllByUserId(userId, pageable);
    }

    public DomainPage<Enroll> getEnrollListByBoardIdAndStatus(Long boardId, AcceptStatus acceptStatus, DomainPageable pageable) {
        return enrollRepository.findAllByBoardIdAndStatus(boardId, acceptStatus, pageable);
    }

    public DomainPage<Long> getBoardIdsWithPendingEnrolls(Long userId, DomainPageable pageable) {
        return enrollRepository.findBoardIdsWithPendingEnrolls(userId, pageable);
    }

    public List<Enroll> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollRepository.findAllByBoardIds(boardIds);
    }

    public long getEnrollPendingCountByBoardWriter(Long userId) {
        return enrollRepository.countByBoardWriterAndStatus(userId, AcceptStatus.PENDING);
    }

    public void updateEnroll(Enroll enroll) {
        enrollRepository.save(enroll);
    }

    public void deleteEnroll(Enroll enroll) {
        enrollRepository.delete(enroll);
    }

    private void validateDuplicateEnroll(User user, Board board) {
        enrollRepository.findByUserAndBoard(user, board)
                .ifPresent(existingEnroll -> {
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.PENDING) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_PENDING);
                    }
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.REJECTED) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_REJECTED);
                    }
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.ACCEPTED) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_ACCEPTED);
                    }
                });
    }
}
