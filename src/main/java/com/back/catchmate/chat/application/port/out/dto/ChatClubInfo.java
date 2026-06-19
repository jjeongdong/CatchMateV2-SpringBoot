package com.back.catchmate.chat.application.port.out.dto;

public record ChatClubInfo(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
