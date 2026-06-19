package com.back.catchmate.enroll.application.dto.response;

public record EnrollClubView(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
