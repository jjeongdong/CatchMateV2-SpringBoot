package com.back.catchmate.club.application.dto.response;

public record ClubInternalResponse(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
