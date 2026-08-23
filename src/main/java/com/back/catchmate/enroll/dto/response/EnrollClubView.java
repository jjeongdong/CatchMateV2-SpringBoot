package com.back.catchmate.enroll.dto.response;

public record EnrollClubView(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
}
