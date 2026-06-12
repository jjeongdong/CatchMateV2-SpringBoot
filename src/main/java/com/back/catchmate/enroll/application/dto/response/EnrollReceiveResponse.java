package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import java.util.List;

public record EnrollReceiveResponse(
        BoardResponse boardResponse,
        List<EnrollResponse> enrollResponses
) {
    public static EnrollReceiveResponse of(BoardResponse boardResponse, List<EnrollResponse> enrollResponses) {
        return new EnrollReceiveResponse(boardResponse, enrollResponses);
    }
}
