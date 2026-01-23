package com.back.catchmate.application.board.dto.response;

import com.back.catchmate.application.club.dto.response.ClubResponse;
import com.back.catchmate.application.user.dto.response.UserResponse; // 혹은 작성자 전용 WriterResponse
import com.back.catchmate.domain.board.model.Board;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardResponse {
    private Long boardId;
    private String title;
    private String content;
    private int currentPerson;
    private int maxPerson;
    private String preferredGender;
    private String preferredAgeRange;
    private LocalDateTime liftUpDate;
    private boolean isBookMarked;
    private String buttonStatus;
    private Long chatRoomId;

    private ClubResponse cheerClub; 
    private GameResponse game;      
    private UserResponse user;      

    public static BoardResponse of(Board board, boolean isBookMarked, String buttonStatus, Long chatRoomId) {
        return BoardResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .currentPerson(board.getCurrentPerson())
                .maxPerson(board.getMaxPerson())
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange())
                .liftUpDate(board.getLiftUpDate())
                .isBookMarked(isBookMarked)
                .buttonStatus(buttonStatus)
                .chatRoomId(chatRoomId)
                .cheerClub(ClubResponse.from(board.getCheerClub()))
                .game(GameResponse.from(board.getGame()))
                .user(UserResponse.from(board.getUser())) 
                .build();
    }
}
