package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.user.domain.model.User;

public interface NoticeFetchPort {
    Notice createNotice(User writer, String title, String content);
    void deleteNotice(Notice notice);
    Notice getNoticeEntity(Long noticeId);
    PagedResponse<NoticeResponse> getNoticeList(int page, int size);
    DomainPage<Notice> getNoticeList(DomainPageable pageable);
    Notice updateNotice(Notice notice);
}
