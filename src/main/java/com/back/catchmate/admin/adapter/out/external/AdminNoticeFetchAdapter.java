package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;
import com.back.catchmate.admin.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.notice.dto.response.NoticeSummary;
import com.back.catchmate.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminNoticeFetchAdapter implements NoticeFetchPort {
    private final NoticeService noticeService;

    @Override
    public AdminNoticeInfo getNotice(Long noticeId) {
        return fromInternalResponse(noticeService.getNoticeSummary(noticeId));
    }

    @Override
    public Page<AdminNoticeInfo> getNoticeList(Pageable pageable) {
        return noticeService.getNoticeSummaries(pageable).map(this::fromInternalResponse);
    }

    private AdminNoticeInfo fromInternalResponse(NoticeSummary response) {
        return new AdminNoticeInfo(
                response.noticeId(),
                response.writerId(),
                response.title(),
                response.content(),
                response.createdAt()
        );
    }
}
