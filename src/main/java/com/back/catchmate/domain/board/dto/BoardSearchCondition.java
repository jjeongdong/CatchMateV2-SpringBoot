package com.back.catchmate.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardSearchCondition {
    private final Long userId;
    private final LocalDate gameDate;
    private final Integer maxPerson;
    private final List<Long> preferredTeamIdList;
    private final List<Long> blockedUserIds;
    private final LocalDateTime lastLiftUpDate;
    private final Long lastBoardId;

    public static BoardSearchCondition of(Long userId,
                                          LocalDate gameDate,
                                          Integer maxPerson,
                                          List<Long> preferredTeamIdList,
                                          List<Long> blockedUserIds) {
        return BoardSearchCondition.builder()
                .userId(userId)
                .gameDate(gameDate)
                .maxPerson(maxPerson)
                .preferredTeamIdList(preferredTeamIdList == null ? List.of() : List.copyOf(preferredTeamIdList))
                .blockedUserIds(blockedUserIds == null ? List.of() : List.copyOf(blockedUserIds))
                .build();
    }

    public static BoardSearchCondition ofCursor(Long userId,
                                                LocalDate gameDate,
                                                Integer maxPerson,
                                                List<Long> preferredTeamIdList,
                                                List<Long> blockedUserIds,
                                                LocalDateTime lastLiftUpDate,
                                                Long lastBoardId) {
        return BoardSearchCondition.builder()
                .userId(userId)
                .gameDate(gameDate)
                .maxPerson(maxPerson)
                .preferredTeamIdList(preferredTeamIdList == null ? List.of() : List.copyOf(preferredTeamIdList))
                .blockedUserIds(blockedUserIds == null ? List.of() : List.copyOf(blockedUserIds))
                .lastLiftUpDate(lastLiftUpDate)
                .lastBoardId(lastBoardId)
                .build();
    }
}
