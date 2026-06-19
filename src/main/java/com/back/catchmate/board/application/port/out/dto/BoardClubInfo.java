package com.back.catchmate.board.application.port.out.dto;

public record BoardClubInfo(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
