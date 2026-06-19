package com.back.catchmate.enroll.application.port.out.dto;

public record EnrollClubInfo(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
