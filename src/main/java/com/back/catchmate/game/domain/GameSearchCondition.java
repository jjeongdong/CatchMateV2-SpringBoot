package com.back.catchmate.game.domain;

import java.time.LocalDate;

/**
 * 경기 선택 화면용 조회 조건. 모든 필드는 선택값(null 이면 해당 필터 없음)이고,
 * clubId 는 홈/원정 어느 쪽이든 매칭한다.
 */
public record GameSearchCondition(LocalDate gameDate, Long clubId) {}
