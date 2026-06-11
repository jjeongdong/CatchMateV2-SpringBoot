package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> preferredAgeRange;
    private LocalDateTime liftUpDate;
    private boolean bookMarked;

    private String buttonStatus;
    private Long myEnrollId;
    private Long chatRoomId;

    private ClubResponse cheerClub;
    private GameResponse game;
    private UserResponse user;

    public static BoardDetailResponse from(Board board, boolean bookMarked, BoardButtonStatus buttonStatus, Long myEnrollId, Long chatRoomId) {
        return BoardDetailResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .currentPerson(board.getCurrentPerson())
                .maxPerson(board.getMaxPerson())
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange().asList())
                .liftUpDate(board.getLiftUpDate())
                .bookMarked(bookMarked)
                .buttonStatus(buttonStatus.name())
                .myEnrollId(myEnrollId)
                .chatRoomId(chatRoomId)
                .cheerClub(ClubResponse.from(board.getCheerClub()))
                .game(GameResponse.from(board.getGame()))
                .user(UserResponse.from(board.getUser()))
                .build();
    }
}
