package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.NoticeFetchPort;
import com.back.catchmate.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.application.service.NoticeService;
import com.back.catchmate.notice.domain.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminNoticeFetchAdapter implements NoticeFetchPort {
    private final NoticeService noticeService;

    @Override
    public Notice createNotice(Long writerId, String title, String content) {
        return noticeService.createNotice(writerId, title, content);
    }

    @Override
    public void deleteNotice(Notice notice) {
        noticeService.deleteNotice(notice);
    }

    @Override
    public Notice getNoticeEntity(Long noticeId) {
        return noticeService.getNoticeEntity(noticeId);
    }

    @Override
    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        return noticeService.getNoticeList(page, size);
    }

    @Override
    public Page<Notice> getNoticeList(Pageable pageable) {
        return noticeService.getNoticeList(pageable);
    }

    @Override
    public Notice updateNotice(Notice notice) {
        return noticeService.updateNotice(notice);
    }
}
