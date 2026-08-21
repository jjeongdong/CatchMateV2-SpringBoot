package com.back.catchmate.club.dto.response;

public record ClubSummary(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
