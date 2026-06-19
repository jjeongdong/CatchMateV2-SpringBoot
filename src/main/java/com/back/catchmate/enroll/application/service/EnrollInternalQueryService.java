package com.back.catchmate.enroll.application.service;

import com.back.catchmate.enroll.application.dto.response.EnrollInternalResponse;
import com.back.catchmate.enroll.application.port.in.EnrollInternalQueryUseCase;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollInternalQueryService implements EnrollInternalQueryUseCase {
    private final EnrollReader enrollReader;

    @Override
    public EnrollInternalResponse getEnroll(Long enrollId) {
        return toInternalResponse(enrollReader.getEnroll(enrollId));
    }

    @Override
    public Optional<EnrollInternalResponse> findEnrollByUserIdAndBoardId(Long userId, Long boardId) {
        return enrollReader.findEnrollByUserIdAndBoardId(userId, boardId)
                .map(this::toInternalResponse);
    }

    @Override
    public Optional<String> findAcceptStatusById(Long enrollId) {
        return enrollReader.findAcceptStatusById(enrollId).map(AcceptStatus::name);
    }

    @Override
    public Map<Long, String> getAcceptStatusMapByIds(List<Long> ids) {
        return enrollReader.getAcceptStatusMapByIds(ids).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()));
    }

    @Override
    public List<EnrollInternalResponse> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollReader.getEnrollListByBoardIds(boardIds).stream()
                .map(this::toInternalResponse)
                .toList();
    }

    private EnrollInternalResponse toInternalResponse(Enroll enroll) {
        return new EnrollInternalResponse(
                enroll.getId(),
                enroll.getUserId(),
                enroll.getBoardId(),
                enroll.getDescription(),
                enroll.getAcceptStatus() != null ? enroll.getAcceptStatus().name() : null,
                enroll.isNewEnroll(),
                enroll.getRequestedAt()
        );
    }
}
