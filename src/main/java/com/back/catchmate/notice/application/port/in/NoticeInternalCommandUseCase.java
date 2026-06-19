package com.back.catchmate.notice.application.port.in;

import com.back.catchmate.notice.application.dto.command.NoticeInternalCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeInternalUpdateCommand;
import com.back.catchmate.notice.application.dto.response.NoticeInternalCreateResponse;

public interface NoticeInternalCommandUseCase {
    NoticeInternalCreateResponse createNotice(NoticeInternalCreateCommand command);

    void updateNotice(NoticeInternalUpdateCommand command);

    void deleteNotice(Long noticeId);
}
