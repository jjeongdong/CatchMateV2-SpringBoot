package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;
import com.back.catchmate.admin.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.notice.application.dto.response.NoticeInternalResponse;
import com.back.catchmate.notice.application.port.in.NoticeAdminQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminNoticeFetchAdapter implements NoticeFetchPort {
    private final NoticeAdminQueryUseCase noticeAdminQueryUseCase;

    @Override
    public AdminNoticeInfo getNotice(Long noticeId) {
        return fromInternalResponse(noticeAdminQueryUseCase.getNotice(noticeId));
    }

    @Override
    public Page<AdminNoticeInfo> getNoticeList(Pageable pageable) {
        return noticeAdminQueryUseCase.getNoticeList(pageable).map(this::fromInternalResponse);
    }

    private AdminNoticeInfo fromInternalResponse(NoticeInternalResponse response) {
        return new AdminNoticeInfo(
                response.noticeId(),
                response.writerId(),
                response.title(),
                response.content(),
                response.createdAt()
        );
    }
}
