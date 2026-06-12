package com.back.catchmate.board.application.dto.response;


public record BoardLiftUpResponse(
        boolean state,
        String remainTime
) {
    public static BoardLiftUpResponse of(boolean state, String remainTime) {
        return new BoardLiftUpResponse(state, remainTime);
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
