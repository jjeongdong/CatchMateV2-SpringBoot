package com.back.catchmate.enroll.dto.response;


public record EnrollCountResponse(
        long count
) {
    public static EnrollCountResponse of(long count) {
        return new EnrollCountResponse(count);
    }
}
