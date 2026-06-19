package com.back.catchmate.board.application.dto.response;

/**
 * board 응답에 임베드되는 club 요약. board 컨텍스트가 자체 소유한 API 계약 — club 컨텍스트의 ClubResponse 변경에 영향받지 않음.
 */
public record BoardClubView(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
