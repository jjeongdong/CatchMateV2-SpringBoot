package com.back.catchmate.user.application.port.out.dto;

public record UserClubInfo(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
