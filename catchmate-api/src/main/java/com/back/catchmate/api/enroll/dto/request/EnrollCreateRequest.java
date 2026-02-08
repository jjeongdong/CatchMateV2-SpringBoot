package com.back.catchmate.api.enroll.dto.request;

import com.back.catchmate.orchestration.enroll.dto.command.EnrollCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollCreateRequest {
    private String description;

    public EnrollCreateCommand toCommand(Long userId, Long boardId) {
        return EnrollCreateCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .description(description)
                .build();
    }
}
