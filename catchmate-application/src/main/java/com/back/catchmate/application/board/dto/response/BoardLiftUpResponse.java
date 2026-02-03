package com.back.catchmate.application.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoardLiftUpResponse {
    private boolean state;
    private String remainTime;

    public static BoardLiftUpResponse of(boolean state, String remainTime) {
        return BoardLiftUpResponse.builder()
                .state(state)
                .remainTime(remainTime)
                .build();
    }

    public static BoardLiftUpResponse fromRemainingMinutes(boolean state, long remainingMinutes) {
        return of(state, formatRemainingTime(remainingMinutes));
    }

    private static String formatRemainingTime(long remainingMinutes) {
        long days = remainingMinutes / 1440;
        long hours = (remainingMinutes % 1440) / 60;
        long minutes = remainingMinutes % 60;

        return String.format("%d일 %02d시간 %02d분", days, hours, minutes);
    }
}
