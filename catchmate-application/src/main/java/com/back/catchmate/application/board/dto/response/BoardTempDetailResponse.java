package com.back.catchmate.application.board.dto.response;

import com.back.catchmate.application.club.dto.response.ClubResponse;
import com.back.catchmate.application.user.dto.response.UserResponse;
import com.back.catchmate.domain.board.model.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoardTempDetailResponse {
    private Long boardId;
    private String title;
    private String content;
    private int maxPerson;
    private String preferredGender;
    private String preferredAgeRange;

    private ClubResponse cheerClub;
    private GameResponse game;
    private UserResponse user;

    public static BoardTempDetailResponse from(Board board) {
        if (board == null) {
            return null;
        }

        return BoardTempDetailResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .maxPerson(board.getMaxPerson())
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange())
                .cheerClub(board.getCheerClub() != null ? ClubResponse.from(board.getCheerClub()) : null)
                .game(board.getGame() != null ? GameResponse.from(board.getGame()) : null)
                .user(board.getUser() != null ? UserResponse.from(board.getUser()) : null)
                .build();
    }
}
