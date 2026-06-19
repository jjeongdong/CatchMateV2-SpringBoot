package com.back.catchmate.board.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardSearchCondition {
    private final Long userId;
    /**
     * 게임 날짜 필터를 미리 game 컨텍스트에서 ID 목록으로 resolve 한 결과.
     * <p>null: 게임 날짜 필터 미적용. 빈 리스트: 매칭 경기 없음(→ 결과도 비어야 함).
     */
    private final List<Long> matchingGameIds;
    private final Integer maxPerson;
    private final List<Long> preferredTeamIdList;
    private final List<Long> blockedUserIds;
    private final LocalDateTime lastLiftUpDate;
    private final Long lastBoardId;

    public static BoardSearchCondition of(Long userId,
                                          List<Long> matchingGameIds,
                                          Integer maxPerson,
                                          List<Long> preferredTeamIdList,
                                          List<Long> blockedUserIds,
                                          LocalDateTime lastLiftUpDate,
                                          Long lastBoardId) {
        return BoardSearchCondition.builder()
                .userId(userId)
                .matchingGameIds(matchingGameIds == null ? null : List.copyOf(matchingGameIds))
                .maxPerson(maxPerson)
                .preferredTeamIdList(preferredTeamIdList == null ? List.of() : List.copyOf(preferredTeamIdList))
                .blockedUserIds(blockedUserIds == null ? List.of() : List.copyOf(blockedUserIds))
                .lastLiftUpDate(lastLiftUpDate)
                .lastBoardId(lastBoardId)
                .build();
    }
}
