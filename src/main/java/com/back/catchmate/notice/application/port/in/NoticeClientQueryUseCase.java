package com.back.catchmate.notice.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;

public interface NoticeClientQueryUseCase {
    NoticeDetailResponse getNoticeDetail(Long noticeId);

    PagedResponse<NoticeResponse> getNoticeList(int page, int size);
}
