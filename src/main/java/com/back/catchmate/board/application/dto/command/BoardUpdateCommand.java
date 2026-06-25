package com.back.catchmate.board.application.dto.command;

import java.util.List;

public record BoardUpdateCommand(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        Long cheerClubId,
        String preferredGender,
        List<String> preferredAgeRange,
        Long gameId,
        boolean completed
) {
}
