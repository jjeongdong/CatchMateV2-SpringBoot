package com.back.catchmate.admin.application.port.in;

import com.back.catchmate.admin.application.dto.command.InquiryRegisterAnswerCommand;
import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminCorpusReindexResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryAnswerResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeActionResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeUpdateResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportActionResponse;

public interface AdminClientCommandUseCase {
    AdminNoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command);

    AdminInquiryAnswerResponse createInquiryAnswer(InquiryRegisterAnswerCommand command);

    AdminReportActionResponse updateReportProcess(Long reportId);

    AdminNoticeUpdateResponse updateNotice(Long noticeId, NoticeUpdateCommand command);

    AdminNoticeActionResponse deleteNotice(Long noticeId);

    AdminCorpusReindexResponse reindexInquiryCorpus();
}
