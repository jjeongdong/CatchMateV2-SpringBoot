package com.back.catchmate.orchestration.board.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BoardUpdateCommand {
    private Long boardId;
    private String title;
    private String content;
    private int maxPerson;
    private Long cheerClubId;
    private String preferredGender;
    private List<String> preferredAgeRange;
    private GameUpdateCommand gameUpdateCommand;
    private boolean completed;
}
