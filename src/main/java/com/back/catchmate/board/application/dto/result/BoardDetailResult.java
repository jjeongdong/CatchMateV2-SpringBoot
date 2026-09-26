package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.application.dto.result.BoardResult.ClubView;
import com.back.catchmate.board.application.dto.result.BoardResult.GameView;
import com.back.catchmate.board.application.dto.result.BoardResult.WriterView;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardButtonStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BoardDetailResult(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        LocalDateTime liftUpDate,
        boolean bookMarked,
        String buttonStatus,
        Long myEnrollId,
        Long chatRoomId,
        ClubView cheerClub,
        GameView game,
        WriterView user) {
    public static BoardDetailResult of(
            Board board,
            boolean bookMarked,
            BoardButtonStatus buttonStatus,
            Long myEnrollId,
            Long chatRoomId,
            WriterView writer,
            ClubView cheerClub,
            GameView game) {
        return new BoardDetailResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                board.getLiftUpDate(),
                bookMarked,
                buttonStatus.name(),
                myEnrollId,
                chatRoomId,
                cheerClub,
                game,
                writer);
    }
}
