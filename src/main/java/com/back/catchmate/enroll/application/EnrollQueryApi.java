package com.back.catchmate.enroll.application;

import com.back.catchmate.enroll.application.dto.api.EnrollInfo;
import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.EnrollRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollQueryApi {
    private final EnrollRepository enrollRepository;

    /**
     * 사용자가 게시글에 낸 신청을 찾는다 (게시글 상세의 버튼 상태용).
     *
     * @param applicantId 신청자 ID
     * @param boardId     게시글 ID
     * @return 신청 정보, 신청하지 않았으면 empty
     */
    @Transactional(readOnly = true)
    public Optional<EnrollInfo> findInfo(Long applicantId, Long boardId) {
        return enrollRepository
                .findByApplicantIdAndBoardId(applicantId, boardId)
                .map(EnrollInfo::from);
    }

    /**
     * 게시글에 들어온 대기 중 신청을 최신순으로 가져온다 (관리자 게시글 상세용).
     *
     * @param boardId 게시글 ID
     * @return 대기 중 신청 목록, 없으면 빈 목록
     */
    @Transactional(readOnly = true)
    public List<EnrollInfo> getPendingInfosByBoardId(Long boardId) {
        return enrollRepository.findPendingByBoardIds(List.of(boardId)).stream()
                .map(EnrollInfo::from)
                .toList();
    }

    /**
     * 여러 신청의 상태 이름을 한 번에 조회한다 (알림 목록용).
     *
     * @param enrollIds 신청 ID 들
     * @return 신청 ID → 상태 이름(PENDING·ACCEPTED·REJECTED). 없는(취소된) 신청은 빠진다. 입력이 비면 조회 없이 빈 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, String> getAcceptStatuses(Collection<Long> enrollIds) {
        if (enrollIds.isEmpty()) {
            return Map.of();
        }
        return enrollRepository.findAcceptStatusesByIds(enrollIds).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().name()));
    }

    /**
     * 신청 하나의 상태 이름을 조회한다 (알림 상세용).
     *
     * @param enrollId 신청 ID
     * @return 상태 이름, 취소된 신청이면 empty
     */
    @Transactional(readOnly = true)
    public Optional<String> findAcceptStatus(Long enrollId) {
        return enrollRepository.findAcceptStatusById(enrollId).map(AcceptStatus::name);
    }
}
