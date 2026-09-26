package com.back.catchmate.board.application.dto.command;

import java.time.LocalDate;
import java.util.List;

// cursor 가 null 이면 첫 페이지
public record BoardSearchCommand(
        LocalDate gameDate, Integer maxPerson, List<Long> preferredTeamIds, String cursor, int size) {}
