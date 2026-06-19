package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.application.port.out.external.NoticeCommandPort;
import com.back.catchmate.notice.application.dto.command.NoticeInternalCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeInternalUpdateCommand;
import com.back.catchmate.notice.application.dto.response.NoticeInternalCreateResponse;
import com.back.catchmate.notice.application.port.in.NoticeInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminNoticeCommandAdapter implements NoticeCommandPort {
    private final NoticeInternalCommandUseCase noticeInternalCommandUseCase;

    @Override
    public AdminNoticeCreateResponse createNotice(Long writerId, NoticeCreateCommand command) {
        NoticeInternalCreateResponse response = noticeInternalCommandUseCase.createNotice(
                new NoticeInternalCreateCommand(writerId, command.title(), command.content())
        );
        return new AdminNoticeCreateResponse(response.noticeId(), response.createdAt());
    }

    @Override
    public void updateNotice(Long noticeId, NoticeUpdateCommand command) {
        noticeInternalCommandUseCase.updateNotice(
                new NoticeInternalUpdateCommand(noticeId, command.title(), command.content())
        );
    }

    @Override
    public void deleteNotice(Long noticeId) {
        noticeInternalCommandUseCase.deleteNotice(noticeId);
    }
}
