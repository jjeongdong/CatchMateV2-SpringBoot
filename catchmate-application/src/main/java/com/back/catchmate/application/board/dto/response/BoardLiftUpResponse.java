package com.back.catchmate.application.board.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardLiftUpResponse {
    private boolean state;
    private String remainTime;

    public static BoardLiftUpResponse of(boolean state, String remainTime) {
        return BoardLiftUpResponse.builder()
                .state(state)
                .remainTime(remainTime)
                .build();
    }
}
