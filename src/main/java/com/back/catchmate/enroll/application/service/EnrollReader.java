package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EnrollReader {
    private final EnrollRepository enrollRepository;

    public Enroll getEnroll(Long enrollId) {
        return enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Optional<Enroll> findEnrollByUserIdAndBoardId(Long userId, Long boardId) {
        return enrollRepository.findByUserIdAndBoardId(userId, boardId);
    }

    public Page<Enroll> getEnrollListByUserId(Long userId, Pageable pageable) {
        return enrollRepository.findAllByUserId(userId, pageable);
    }

    public Page<Enroll> getEnrollListByBoardIdAndStatus(Long boardId, AcceptStatus acceptStatus, Pageable pageable) {
        return enrollRepository.findAllByBoardIdAndStatus(boardId, acceptStatus, pageable);
    }

    public Page<Long> getBoardIdsWithPendingEnrolls(Long userId, Pageable pageable) {
        return enrollRepository.findBoardIdsWithPendingEnrolls(userId, pageable);
    }

    public List<Enroll> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollRepository.findAllByBoardIds(boardIds);
    }

    public Map<Long, AcceptStatus> getAcceptStatusMapByIds(List<Long> enrollIds) {
        return enrollRepository.findAcceptStatusMapByIds(enrollIds);
    }

    public Optional<AcceptStatus> findAcceptStatusById(Long enrollId) {
        return enrollRepository.findAcceptStatusById(enrollId);
    }

    public long getEnrollPendingCountByBoardWriter(Long userId) {
        return enrollRepository.countByBoardWriterAndStatus(userId, AcceptStatus.PENDING);
    }

    public List<Enroll> getAcceptedEnrollsBetween(Long applicantId, Long boardOwnerId) {
        return enrollRepository.findAllByApplicantAndBoardOwnerAndStatus(applicantId, boardOwnerId, AcceptStatus.ACCEPTED);
    }

    public void checkDuplicateEnroll(Long userId, Long boardId) {
        enrollRepository.findByUserIdAndBoardId(userId, boardId)
                .ifPresent(Enroll::preventNewEnroll);
    }
}
