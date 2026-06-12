package com.back.catchmate.admin.application.service;

import com.back.catchmate.admin.application.port.out.NoticeFetchPort;

import com.back.catchmate.admin.application.port.out.UserFetchPort;

import com.back.catchmate.admin.application.port.out.InquiryFetchPort;

import com.back.catchmate.admin.application.port.out.BoardFetchPort;

import com.back.catchmate.admin.application.port.out.EnrollFetchPort;

import com.back.catchmate.admin.application.port.out.ReportFetchPort;

import com.back.catchmate.admin.application.event.AdminInquiryAnswerNotificationEvent;
import com.back.catchmate.admin.application.event.AdminNoticeCreateNotificationEvent;
import com.back.catchmate.admin.application.port.in.AdminUseCase;
import com.back.catchmate.admin.application.port.out.GameFetchPort;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.game.domain.model.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.back.catchmate.common.response.PagedResponse;
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

    private final BoardFetchPort boardFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final GameFetchPort gameFetchPort;
    private final InquiryFetchPort inquiryFetchPort;
    private final NoticeFetchPort noticeFetchPort;
    private final ReportFetchPort reportFetchPort;
    private final UserFetchPort userFetchPort;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        Notice savedNotice = noticeFetchPort.createNotice(userId, command.title(), command.content());

        List<User> recipients = userFetchPort.getEventAlarmEnabledUsers();
        applicationEventPublisher.publishEvent(AdminNoticeCreateNotificationEvent.of(savedNotice, recipients));

        return NoticeCreateResponse.from(savedNotice);
    }

    @Transactional
    public InquiryAnswerResponse createInquiryAnswer(InquiryAnswerCommand command) {
        Inquiry inquiry = inquiryFetchPort.getInquiryEntity(command.inquiryId());

        inquiry.registerAnswer(command.content());
        Inquiry updatedInquiry = inquiryFetchPort.updateInquiry(inquiry);

        User recipient = userFetchPort.getUser(updatedInquiry.getUserId());
        applicationEventPublisher.publishEvent(AdminInquiryAnswerNotificationEvent.of(
                recipient,
                updatedInquiry
        ));

        return InquiryAnswerResponse.of(updatedInquiry.getId(), updatedInquiry.getUserId());
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
        Pageable domainPageable = PageRequest.of(page, size);
        Page<User> userPage = userFetchPort.getUsersByClub(clubName, domainPageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    public AdminBoardDetailWithEnrollResponse getBoardWithEnrollList(Long boardId) {
        Board board = boardFetchPort.getCompletedBoard(boardId);
        List<Enroll> enrolls = enrollFetchPort.getEnrollListByBoardIds(Collections.singletonList(boardId));

        java.util.List<Long> enrollUserIds = enrolls.stream()
                .map(Enroll::getUserId)
                .distinct()
                .toList();
        java.util.Map<Long, User> enrollUserById = enrollUserIds.isEmpty()
                ? java.util.Map.of()
                : userFetchPort.getUsers(enrollUserIds).stream()
                        .collect(Collectors.toMap(User::getId, java.util.function.Function.identity()));

        List<AdminEnrollmentResponse> enrollmentInfos = enrolls.stream()
                .map(enroll -> AdminEnrollmentResponse.from(enroll, enrollUserById.get(enroll.getUserId())))
                .toList();

        User writer = board.getUserId() != null ? userFetchPort.getUser(board.getUserId()) : null;
        Game game = board.getGameId() != null ? gameFetchPort.getGame(board.getGameId()) : null;

        return AdminBoardDetailWithEnrollResponse.from(board, writer, game, enrollmentInfos);
    }

    public PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Board> boardPage = boardFetchPort.getBoardListByUserId(userId, domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminBoardResponse> getBoardList(int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Board> boardPage = boardFetchPort.getBoardList(domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public AdminReportDetailResponse getReport(Long reportId) {
        Report report = reportFetchPort.getReport(reportId);
        User reporter = userFetchPort.getUser(report.getReporterId());
        User reportedUser = userFetchPort.getUser(report.getReportedUserId());
        return AdminReportDetailResponse.from(report, reporter, reportedUser);
    }

    public PagedResponse<AdminReportResponse> getReportList(int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Report> reportPage = reportFetchPort.getReportList(domainPageable);

        java.util.Map<Long, User> reporterById = userFetchPort.getUsers(
                reportPage.getContent().stream().map(Report::getReporterId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(r -> AdminReportResponse.from(r, reporterById.get(r.getReporterId())))
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryFetchPort.getInquiryEntity(inquiryId);
        User user = userFetchPort.getUser(inquiry.getUserId());
        return AdminInquiryDetailResponse.from(inquiry, user);
    }

    public PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Inquiry> inquiryPage = inquiryFetchPort.getInquiryList(domainPageable);

        java.util.Map<Long, User> userById = userFetchPort.getUsers(
                inquiryPage.getContent().stream().map(Inquiry::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(i -> AdminInquiryResponse.from(i, userById.get(i.getUserId())))
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        User writer = userFetchPort.getUser(notice.getWriterId());
        return AdminNoticeDetailResponse.from(notice, writer.getNickName());
    }

    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Notice> noticePage = noticeFetchPort.getNoticeList(domainPageable);

        java.util.Map<Long, String> writerNicknameById = userFetchPort.getUsers(
                noticePage.getContent().stream().map(Notice::getWriterId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getNickName));

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> AdminNoticeResponse.from(n, writerNicknameById.getOrDefault(n.getWriterId(), "")))
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    @Transactional
    public ReportActionResponse updateReportProcess(Long reportId) {
        Report report = reportFetchPort.getReport(reportId);
        User reportedUser = userFetchPort.getUser(report.getReportedUserId());

        reportedUser.markAsReported();
        userFetchPort.updateUser(reportedUser);

        report.process();
        reportFetchPort.updateReport(report);

        return ReportActionResponse.of(report.getId(), reportedUser.getId());
    }

    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        notice.updateNotice(command.title(), command.content());
        Notice updatedNotice = noticeFetchPort.updateNotice(notice);
        User writer = userFetchPort.getUser(updatedNotice.getWriterId());
        return NoticeDetailResponse.from(updatedNotice, writer.getNickName());
    }

    @Transactional
    public NoticeActionResponse deleteNotice(Long noticeId) {
        Notice notice = noticeFetchPort.getNoticeEntity(noticeId);
        noticeFetchPort.deleteNotice(notice);
        return NoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }
}
