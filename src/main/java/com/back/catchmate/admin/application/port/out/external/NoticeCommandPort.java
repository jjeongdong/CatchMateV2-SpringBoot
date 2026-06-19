package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;

public interface NoticeCommandPort {
    AdminNoticeCreateResponse createNotice(Long writerId, NoticeCreateCommand command);

    void updateNotice(Long noticeId, NoticeUpdateCommand command);

    void deleteNotice(Long noticeId);
}
