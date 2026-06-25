package com.back.catchmate.game.domain.dto;

import java.time.LocalDate;

/**
 * 프론트 경기 선택 화면용 조회 조건. 모든 필드는 선택값(null 허용)으로 동적 필터링한다.
 * clubId 는 홈/원정 어느 쪽이든 매칭한다.
 */
public record GameSearchCondition(
        LocalDate gameDate,
        Long clubId
) {
}
