package com.back.catchmate.admin.application.service;

import com.back.catchmate.admin.application.port.out.NoticeFetchPort;

import com.back.catchmate.admin.application.port.out.UserFetchPort;

import com.back.catchmate.admin.application.port.out.InquiryFetchPort;

import com.back.catchmate.admin.application.port.out.BoardFetchPort;

import com.back.catchmate.admin.application.port.out.EnrollFetchPort;

import com.back.catchmate.admin.application.port.out.ReportFetchPort;


import com.back.catchmate.admin.application.port.in.AdminUseCase;
import com.back.catchmate.admin.application.event.AdminInquiryAnswerNotificationEvent;
import com.back.catchmate.admin.application.event.AdminNoticeCreateNotificationEvent;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.admin.application.dto.command.InquiryAnswerCommand;
import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.admin.application.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.application.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.application.dto.response.AdminEnrollmentResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserResponse;
import com.back.catchmate.admin.application.dto.response.InquiryAnswerResponse;
import com.back.catchmate.admin.application.dto.response.NoticeActionResponse;
import com.back.catchmate.admin.application.dto.response.NoticeCreateResponse;
import com.back.catchmate.admin.application.dto.response.ReportActionResponse;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {
    private final NoticeFetchPort noticeFetchPort;
    private final UserFetchPort userFetchPort;
    private final InquiryFetchPort inquiryFetchPort;
    private final BoardFetchPort boardFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final ReportFetchPort reportFetchPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        User writer = userFetchPort.getUser(userId);
        Notice savedNotice = noticeFetchPort.createNotice(writer, command.getTitle(), command.getContent());

        List<User> recipients = userFetchPort.getEventAlarmEnabledUsers();
        applicationEventPublisher.publishEvent(AdminNoticeCreateNotificationEvent.of(savedNotice, recipients));

        return NoticeCreateResponse.from(savedNotice);
    }

    @Transactional
    public InquiryAnswerResponse createInquiryAnswer(InquiryAnswerCommand command) {
        Inquiry inquiry = inquiryFetchPort.getInquiryEntity(command.getInquiryId());

        inquiry.registerAnswer(command.getContent());
        Inquiry updatedInquiry = inquiryFetchPort.updateInquiry(inquiry);

        applicationEventPublisher.publishEvent(AdminInquiryAnswerNotificationEvent.of(
                updatedInquiry.getUser(),
                updatedInquiry
        ));

        return InquiryAnswerResponse.of(updatedInquiry.getId(), updatedInquiry.getUser().getId());
    }

    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.of(
                userFetchPort.getTotalUserCount(),
                AdminDashboardResponse.GenderRatio.of(
                        userFetchPort.getUserCountByGender('M'),
                        userFetchPort.getUserCountByGender('F')
                ),
                boardFetchPort.getTotalBoardCount(),
                userFetchPort.getUserCountByClub(),
                userFetchPort.getUserCountByWatchStyle(),
                reportFetchPort.getTotalReportCount(),
                reportFetchPort.getPendingReportCount(),
                inquiryFetchPort.getTotalInquiryCount(),
                inquiryFetchPort.getWaitingInquiryCount()
        );
    }

    public AdminUserDetailResponse getUser(Long userId) {
        User user = userFetchPort.getUser(userId);
        return AdminUserDetailResponse.from(user);
    }

    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<User> userPage = userFetchPort.getUsersByClub(clubName, domainPageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    public AdminBoardDetailWithEnrollResponse getBoardWithEnrollList(Long boardId) {
        Board board = boardFetchPort.getCompletedBoard(boardId);
        List<Enroll> enrolls = enrollFetchPort.getEnrollListByBoardIds(Collections.singletonList(boardId));
        
        List<AdminEnrollmentResponse> enrollmentInfos = enrolls.stream()
                .map(AdminEnrollmentResponse::from)
                .toList();

        return AdminBoardDetailWithEnrollResponse.from(board, enrollmentInfos);
    }

    public PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardFetchPort.getBoardListByUserId(userId, domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminBoardResponse> getBoardList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardFetchPort.getBoardList(domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public AdminReportDetailResponse getReport(Long reportId) {
        Report report = reportFetchPort.getReport(reportId);
        return AdminReportDetailResponse.from(report);
    }

    public PagedResponse<AdminReportResponse> getReportList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Report> reportPage = reportFetchPort.getReportList(domainPageable);

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(AdminReportResponse::from)
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryFetchPort.getInquiryEntity(inquiryId);
        return AdminInquiryDetailResponse.from(inquiry);
    }

    public PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Inquiry> inquiryPage = inquiryFetchPort.getInquiryList(domainPageable);

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(AdminInquiryResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        return AdminNoticeDetailResponse.from(notice);
    }

    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Notice> noticePage = noticeFetchPort.getNoticeList(domainPageable);

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(AdminNoticeResponse::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    @Transactional
    public ReportActionResponse updateReportProcess(Long reportId) {
        // 1. 필요한 도메인 객체 조회 (Orchestration)
        Report report = reportFetchPort.getReport(reportId);
        User reportedUser = report.getReportedUser();

        // 2. 도메인 로직 실행
        reportedUser.markAsReported();
        userFetchPort.updateUser(reportedUser);

        report.process();
        reportFetchPort.updateReport(report);

        return ReportActionResponse.of(report.getId(), reportedUser.getId());
    }

    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        notice.updateNotice(command.getTitle(), command.getContent());
        Notice updatedNotice = noticeFetchPort.updateNotice(notice);
        return NoticeDetailResponse.from(updatedNotice);
    }

    @Transactional
    public NoticeActionResponse deleteNotice(Long noticeId) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        noticeFetchPort.deleteNotice(notice);
        return NoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }
}
