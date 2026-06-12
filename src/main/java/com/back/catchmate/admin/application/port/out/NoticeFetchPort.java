package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.domain.model.Notice;

public interface NoticeFetchPort {
    Notice createNotice(Long writerId, String title, String content);
    void deleteNotice(Notice notice);
    Notice getNoticeEntity(Long noticeId);
    PagedResponse<NoticeResponse> getNoticeList(int page, int size);
    Page<Notice> getNoticeList(Pageable pageable);
    Notice updateNotice(Notice notice);
}
