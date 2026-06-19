package com.back.catchmate.enroll.application.dto.response;

import java.util.List;

public record EnrollReceiveResponse(
        EnrollBoardSummary boardResponse,
        List<EnrollResponse> enrollResponses
) {
    public static EnrollReceiveResponse of(EnrollBoardSummary boardResponse, List<EnrollResponse> enrollResponses) {
        return new EnrollReceiveResponse(boardResponse, enrollResponses);
    }
}
