package com.back.catchmate.game.application.dto;

/**
 * game 컨텍스트가 club 컨텍스트로부터 받아오는 구단 요약. 자체 소유 record(안티-커럽션 레이어).
 */
public record GameClubInfo(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
