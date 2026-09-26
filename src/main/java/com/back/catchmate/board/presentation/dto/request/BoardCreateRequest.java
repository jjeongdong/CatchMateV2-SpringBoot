package com.back.catchmate.board.presentation.dto.request;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 임시저장은 빈 값을 허용하므로 필수 항목 검증은 발행 시 엔티티가 한다.
public record BoardCreateRequest(
        String title,
        String content,
        Integer maxPerson,
        Long cheerClubId,
        String preferredGender,
        List<String> preferredAgeRange,
        @NotNull(message = "임시저장 여부는 필수입니다.") Boolean completed,
        Long gameId) {
    public BoardCreateCommand toCommand() {
        return new BoardCreateCommand(
                title,
                content,
                maxPerson != null ? maxPerson : 0,
                cheerClubId,
                preferredGender,
                preferredAgeRange,
                gameId,
                completed);
    }
}
