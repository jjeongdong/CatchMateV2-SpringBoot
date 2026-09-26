package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.application.dto.result.BoardResult.ClubView;
import com.back.catchmate.board.application.dto.result.BoardResult.GameView;
import com.back.catchmate.board.application.dto.result.BoardResult.WriterView;
import com.back.catchmate.board.domain.Board;
import java.util.List;

public record BoardDraftResult(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        ClubView cheerClub,
        GameView game,
        WriterView user) {
    public static BoardDraftResult of(Board board, WriterView writer, ClubView cheerClub, GameView game) {
        return new BoardDraftResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                cheerClub,
                game,
                writer);
    }
}
