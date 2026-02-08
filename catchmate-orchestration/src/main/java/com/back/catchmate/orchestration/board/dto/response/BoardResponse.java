package com.back.catchmate.orchestration.board.dto.response;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.orchestration.club.dto.response.ClubResponse;
import com.back.catchmate.orchestration.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoardResponse {
    private Long boardId;
    private String title;
    private String content;
    private int currentPerson;
    private int maxPerson;
    private boolean bookMarked;

    private ClubResponse cheerClub;
    private GameResponse gameResponse;
    private UserResponse userResponse;

    public static BoardResponse from(Board board, boolean bookMarked) {
        return BoardResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .currentPerson(board.getCurrentPerson())
                .maxPerson(board.getMaxPerson())
                .bookMarked(bookMarked)
                .cheerClub(ClubResponse.from(board.getCheerClub()))
                .gameResponse(GameResponse.from(board.getGame()))
                .userResponse(UserResponse.from(board.getUser()))
                .build();
    }
}
