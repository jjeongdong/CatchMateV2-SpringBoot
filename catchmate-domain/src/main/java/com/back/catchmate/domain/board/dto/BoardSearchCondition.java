package com.back.catchmate.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class BoardSearchCondition {
    private final Long userId;
    private final LocalDate gameDate;
    private final Integer maxPerson;
    private final List<Long> preferredTeamIdList;
    private final List<Long> blockedUserIds;

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

}
