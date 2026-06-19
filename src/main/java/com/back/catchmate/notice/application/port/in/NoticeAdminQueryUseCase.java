package com.back.catchmate.notice.application.port.in;

import com.back.catchmate.notice.application.dto.response.NoticeInternalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeAdminQueryUseCase {
    NoticeInternalResponse getNotice(Long noticeId);

    Page<NoticeInternalResponse> getNoticeList(Pageable pageable);
}
