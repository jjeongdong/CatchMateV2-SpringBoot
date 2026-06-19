package com.back.catchmate.enroll.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollApplicantResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCountResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollDetailResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollReceiveResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRequestResponse;

public interface EnrollClientQueryUseCase {
    EnrollDetailResponse getEnroll(Long userId, Long enrollId);

    PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size);

    PagedResponse<EnrollApplicantResponse> getEnrollReceiveListByBoardId(Long userId, Long boardId, int page, int size);

    PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size);

    EnrollCountResponse getEnrollPendingCount(Long userId);
}
