package com.back.catchmate.board.application.dto.result;

// remainingTime: 아직 끌어올릴 수 없을 때 "N일 HH시간 MM분", 끌어올렸으면 null
public record BoardLiftUpResult(boolean liftedUp, String remainingTime) {
    public static BoardLiftUpResult lifted() {
        return new BoardLiftUpResult(true, null);
    }

    public static BoardLiftUpResult tooEarly(long remainingMinutes) {
        long days = remainingMinutes / 1440;
        long hours = (remainingMinutes % 1440) / 60;
        long minutes = remainingMinutes % 60;
        return new BoardLiftUpResult(false, String.format("%d일 %02d시간 %02d분", days, hours, minutes));
    }
}
