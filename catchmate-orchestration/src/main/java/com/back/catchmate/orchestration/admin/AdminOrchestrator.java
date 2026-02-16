package com.back.catchmate.orchestration.admin;

import com.back.catchmate.application.admin.event.AdminInquiryAnswerNotificationEvent;
import com.back.catchmate.application.board.service.BoardService;
import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.application.inquiry.service.InquiryService;
import com.back.catchmate.application.notice.service.NoticeService;
import com.back.catchmate.application.report.service.ReportService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.admin.dto.command.InquiryAnswerCommand;
import com.back.catchmate.orchestration.admin.dto.command.NoticeCreateCommand;
import com.back.catchmate.orchestration.admin.dto.command.NoticeUpdateCommand;
import com.back.catchmate.orchestration.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminEnrollmentResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminNoticeResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminReportDetailResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminReportResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminUserResponse;
import com.back.catchmate.orchestration.admin.dto.response.InquiryAnswerResponse;
import com.back.catchmate.orchestration.admin.dto.response.NoticeActionResponse;
import com.back.catchmate.orchestration.admin.dto.response.NoticeCreateResponse;
import com.back.catchmate.orchestration.admin.dto.response.ReportActionResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.notice.dto.response.NoticeDetailResponse;
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
public class AdminOrchestrator {
    private final UserService userService;
    private final BoardService boardService;
    private final EnrollService enrollService;
    private final NoticeService noticeService;
    private final ReportService reportService;
    private final InquiryService inquiryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        User writer = userService.getUser(userId);
        Notice savedNotice = noticeService.createNotice(writer, command.getTitle(), command.getContent());
        return NoticeCreateResponse.from(savedNotice);
    }

    @Transactional
    public InquiryAnswerResponse createInquiryAnswer(InquiryAnswerCommand command) {
        Inquiry inquiry = inquiryService.getInquiry(command.getInquiryId());

        inquiry.registerAnswer(command.getContent());
        Inquiry updatedInquiry = inquiryService.updateInquiry(inquiry);

        applicationEventPublisher.publishEvent(AdminInquiryAnswerNotificationEvent.of(
                updatedInquiry.getUser(),
                updatedInquiry
        ));

        return InquiryAnswerResponse.of(updatedInquiry.getId(), updatedInquiry.getUser().getId());
    }

    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.of(
                userService.getTotalUserCount(),
                AdminDashboardResponse.GenderRatio.of(
                        userService.getUserCountByGender('M'),
                        userService.getUserCountByGender('F')
                ),
                boardService.getTotalBoardCount(),
                userService.getUserCountByClub(),
                userService.getUserCountByWatchStyle(),
                reportService.getTotalReportCount(),
                inquiryService.getTotalInquiryCount()
        );
    }

    public AdminUserDetailResponse getUser(Long userId) {
        User user = userService.getUser(userId);
        return AdminUserDetailResponse.from(user);
    }

    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<User> userPage = userService.getUsersByClub(clubName, domainPageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    public AdminBoardDetailWithEnrollResponse getBoardWithEnrollList(Long boardId) {
        Board board = boardService.getCompletedBoard(boardId);
        List<Enroll> enrolls = enrollService.getEnrollListByBoardIds(Collections.singletonList(boardId));
        
        List<AdminEnrollmentResponse> enrollmentInfos = enrolls.stream()
                .map(AdminEnrollmentResponse::from)
                .toList();

        return AdminBoardDetailWithEnrollResponse.from(board, enrollmentInfos);
    }

    public PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardListByUserId(userId, domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminBoardResponse> getBoardList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public AdminReportDetailResponse getReport(Long reportId) {
        Report report = reportService.getReport(reportId);
        return AdminReportDetailResponse.from(report);
    }

    public PagedResponse<AdminReportResponse> getReportList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Report> reportPage = reportService.getReportList(domainPageable);

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(AdminReportResponse::from)
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);
        return AdminInquiryDetailResponse.from(inquiry);
    }

    public PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Inquiry> inquiryPage = inquiryService.getInquiryList(domainPageable);

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(AdminInquiryResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeService.getNotice(noticeId);
        return AdminNoticeDetailResponse.from(notice);
    }

    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Notice> noticePage = noticeService.getNoticeList(domainPageable);

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(AdminNoticeResponse::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    @Transactional
    public ReportActionResponse updateReportProcess(Long reportId) {
        // 1. 필요한 도메인 객체 조회 (Orchestration)
        Report report = reportService.getReport(reportId);
        User reportedUser = report.getReportedUser();

        // 2. 도메인 로직 실행
        reportedUser.markAsReported();
        userService.updateUser(reportedUser);

        report.process();
        reportService.updateReport(report);

        return ReportActionResponse.of(report.getId(), reportedUser.getId());
    }

    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = noticeService.getNotice(noticeId);
        notice.updateNotice(command.getTitle(), command.getContent());
        Notice updatedNotice = noticeService.updateNotice(notice);
        return NoticeDetailResponse.from(updatedNotice);
    }

    @Transactional
    public NoticeActionResponse deleteNotice(Long noticeId) {
        Notice notice = noticeService.getNotice(noticeId);
        noticeService.deleteNotice(notice);
        return NoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }
}
