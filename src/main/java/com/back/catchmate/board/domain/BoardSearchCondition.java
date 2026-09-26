package com.back.catchmate.board.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 커서 목록 조건.
 *
 * @param matchingGameIds 경기 날짜 필터를 game BC 에서 ID 로 바꾼 결과. null 이면 필터 없음 (빈 목록은 호출 전에 걸러진다)
 * @param writerId        유저별 목록일 때 작성자. null 이면 전체
 * @param lastLiftUpDate  커서 — 이전 페이지 마지막 글의 끌어올린 시각. null 이면 첫 페이지
 * @param lastBoardId     커서 — 같은 시각일 때의 순서 키
 */
public record BoardSearchCondition(
        List<Long> matchingGameIds,
        Integer maxPerson,
        List<Long> preferredTeamIds,
        List<Long> blockedUserIds,
        Long writerId,
        LocalDateTime lastLiftUpDate,
        Long lastBoardId) {
    public BoardSearchCondition {
        matchingGameIds = matchingGameIds == null ? null : List.copyOf(matchingGameIds);
        preferredTeamIds = preferredTeamIds == null ? List.of() : List.copyOf(preferredTeamIds);
        blockedUserIds = blockedUserIds == null ? List.of() : List.copyOf(blockedUserIds);
    }
}
