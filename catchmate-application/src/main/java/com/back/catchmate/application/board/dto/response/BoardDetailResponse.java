package com.back.catchmate.application.board.dto.response;

import com.back.catchmate.application.club.dto.response.ClubResponse;
import com.back.catchmate.application.user.dto.response.UserResponse;
import com.back.catchmate.domain.board.model.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BoardDetailResponse {
    private Long boardId;
    private String title;
    private String content;
    private int currentPerson;
    private int maxPerson;
    private String preferredGender;
    private String preferredAgeRange;
    private LocalDateTime liftUpDate;
    private boolean bookMarked;

    private String buttonStatus;
    private Long myEnrollId;
    private Long chatRoomId;

    private ClubResponse cheerClub;
    private GameResponse game;
    private UserResponse user;

    public static BoardDetailResponse from(Board board, boolean bookMarked, String buttonStatus, Long myEnrollId, Long chatRoomId) {
        return BoardDetailResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .currentPerson(board.getCurrentPerson())
                .maxPerson(board.getMaxPerson())
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange())
                .liftUpDate(board.getLiftUpDate())
                .bookMarked(bookMarked)
                .buttonStatus(buttonStatus)
                .myEnrollId(myEnrollId)
                .chatRoomId(chatRoomId)
                .cheerClub(ClubResponse.from(board.getCheerClub()))
                .game(GameResponse.from(board.getGame()))
                .user(UserResponse.from(board.getUser()))
                .build();
    }
}
