package com.back.catchmate.admin.application.service;

import com.back.catchmate.admin.application.dto.command.InquiryRegisterAnswerCommand;
import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminCorpusReindexResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryAnswerResponse;
import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import com.back.catchmate.admin.application.dto.response.AdminNoticeActionResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;
import com.back.catchmate.admin.application.dto.response.AdminNoticeUpdateResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportActionResponse;
import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.admin.application.event.NoticeCreatedEvent;
import com.back.catchmate.admin.application.port.in.AdminClientCommandUseCase;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;
import com.back.catchmate.admin.application.port.out.external.AssistIndexCommandPort;
import com.back.catchmate.admin.application.port.out.external.InquiryCommandPort;
import com.back.catchmate.admin.application.port.out.external.InquiryFetchPort;
import com.back.catchmate.admin.application.port.out.external.NoticeCommandPort;
import com.back.catchmate.admin.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.admin.application.port.out.external.ReportCommandPort;
import com.back.catchmate.admin.application.port.out.external.ReportFetchPort;
import com.back.catchmate.admin.application.port.out.external.UserCommandPort;
import com.back.catchmate.admin.application.port.out.external.UserFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminClientCommandService implements AdminClientCommandUseCase {
    private final UserFetchPort userFetchPort;
    private final NoticeFetchPort noticeFetchPort;
    private final ReportFetchPort reportFetchPort;
    private final UserCommandPort userCommandPort;
    private final InquiryFetchPort inquiryFetchPort;
    private final NoticeCommandPort noticeCommandPort;
    private final ReportCommandPort reportCommandPort;
    private final InquiryCommandPort inquiryCommandPort;
    private final AssistIndexCommandPort assistIndexCommandPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AdminNoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        AdminNoticeCreateResponse response = noticeCommandPort.createNotice(userId, command);

        applicationEventPublisher.publishEvent(
                NoticeCreatedEvent.of(response.noticeId(), command.title())
        );

        return response;
    }

    @Override
    public AdminInquiryAnswerResponse createInquiryAnswer(InquiryRegisterAnswerCommand command) {
        inquiryCommandPort.registerAnswer(command);

        AdminInquiryInfo updatedInquiry = inquiryFetchPort.getInquiry(command.inquiryId());
        applicationEventPublisher.publishEvent(
                InquiryAnswerRegisteredEvent.of(updatedInquiry.inquiryId(), updatedInquiry.userId())
        );

        return AdminInquiryAnswerResponse.of(updatedInquiry.inquiryId(), updatedInquiry.userId());
    }

    @Override
    public AdminCorpusReindexResponse reindexInquiryCorpus() {
        return AdminCorpusReindexResponse.of(assistIndexCommandPort.reindex());
    }

    @Override
    public AdminReportActionResponse updateReportProcess(Long reportId) {
        AdminReportInfo report = reportFetchPort.getReport(reportId);
        Long reportedUserId = report.reportedUserId();

        userCommandPort.markUserAsReported(reportedUserId);
        reportCommandPort.processReport(reportId);

        return AdminReportActionResponse.of(reportId, reportedUserId);
    }

    @Override
    public AdminNoticeUpdateResponse updateNotice(Long noticeId, NoticeUpdateCommand command) {
        noticeCommandPort.updateNotice(noticeId, command);

        AdminNoticeInfo updatedNotice = noticeFetchPort.getNotice(noticeId);
        AdminUserInfo writer = userFetchPort.getUser(updatedNotice.writerId());
        return AdminNoticeUpdateResponse.from(updatedNotice, writer.nickName());
    }

    @Override
    public AdminNoticeActionResponse deleteNotice(Long noticeId) {
        noticeCommandPort.deleteNotice(noticeId);
        return AdminNoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }
}
