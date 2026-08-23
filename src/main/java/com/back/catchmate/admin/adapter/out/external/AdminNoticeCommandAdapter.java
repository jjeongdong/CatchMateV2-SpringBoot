package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.application.port.out.external.NoticeCommandPort;
import com.back.catchmate.notice.dto.response.NoticeCreateResponse;
import com.back.catchmate.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminNoticeCommandAdapter implements NoticeCommandPort {
    private final NoticeService noticeService;

    @Override
    public AdminNoticeCreateResponse createNotice(Long writerId, NoticeCreateCommand command) {
        NoticeCreateResponse response = noticeService.createNotice(writerId, command.title(), command.content());
        return new AdminNoticeCreateResponse(response.noticeId(), response.createdAt());
    }

    @Override
    public void updateNotice(Long noticeId, NoticeUpdateCommand command) {
        noticeService.updateNotice(noticeId, command.title(), command.content());
    }

    @Override
    public void deleteNotice(Long noticeId) {
        noticeService.deleteNotice(noticeId);
    }
}
