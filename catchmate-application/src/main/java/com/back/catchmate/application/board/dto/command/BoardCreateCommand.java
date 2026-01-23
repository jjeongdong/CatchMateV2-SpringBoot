package com.back.catchmate.application.board.dto.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardCreateCommand {
    private Long boardId;
    private final String title;
    private final String content;
    private final int maxPerson;
    private final Long cheerClubId;
    private final String preferredGender;
    private final List<String> preferredAgeRange;
    private final GameCreateCommand gameCreateCommand;
    private boolean completed;
}
