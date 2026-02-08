package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.board.model.Board;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminBoardDetailWithEnrollResponse {
    private Long boardId;
    private String title;
    private String content;
    private String writerNickname;
    private LocalDateTime gameStartDate;
    private String location;
    private int maxPerson;
    private int currentPerson;
    private boolean completed;
    private LocalDateTime createdAt;

    private List<AdminEnrollmentResponse> enrollments;

    public static AdminBoardDetailWithEnrollResponse from(Board board, List<AdminEnrollmentResponse> enrollments) {
        return AdminBoardDetailWithEnrollResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .writerNickname(board.getUser().getNickName())
                .gameStartDate(board.getGame().getGameStartDate())
                .location(board.getGame().getLocation())
                .maxPerson(board.getMaxPerson())
                .currentPerson(board.getCurrentPerson())
                .completed(board.isCompleted())
                .createdAt(board.getCreatedAt())
                .enrollments(enrollments)
                .build();
    }
}
