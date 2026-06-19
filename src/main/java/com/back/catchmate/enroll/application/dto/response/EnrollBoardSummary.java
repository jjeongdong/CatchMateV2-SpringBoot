package com.back.catchmate.enroll.application.dto.response;

public record EnrollBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        EnrollClubView cheerClub,
        EnrollGameView gameResponse,
        EnrollWriterView userResponse
) {
}
