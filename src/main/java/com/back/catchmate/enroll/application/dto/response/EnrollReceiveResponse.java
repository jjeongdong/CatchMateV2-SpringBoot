package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EnrollReceiveResponse {
    private BoardResponse boardResponse;
    private List<EnrollResponse> enrollResponses;

    public static EnrollReceiveResponse of(BoardResponse boardResponse, List<EnrollResponse> enrollResponses) {
        return EnrollReceiveResponse.builder()
                .boardResponse(boardResponse)
                .enrollResponses(enrollResponses)
                .build();
    }
}
