package com.back.catchmate.application.admin;

import com.back.catchmate.application.admin.dto.command.InquiryAnswerCommand;
import com.back.catchmate.application.admin.dto.command.NoticeCreateCommand;
import com.back.catchmate.application.admin.dto.command.NoticeUpdateCommand;
import com.back.catchmate.application.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.application.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.application.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.application.admin.dto.response.AdminEnrollmentResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.application.admin.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminNoticeResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserResponse;
import com.back.catchmate.application.admin.dto.response.InquiryAnswerResponse;
import com.back.catchmate.application.admin.dto.response.NoticeActionResponse;
import com.back.catchmate.application.admin.dto.response.NoticeCreateResponse;
import com.back.catchmate.application.admin.dto.response.ReportActionResponse;
import com.back.catchmate.application.admin.event.AdminInquiryAnswerNotificationEvent;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.notice.dto.response.NoticeDetailResponse;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.service.EnrollService;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.service.InquiryService;
import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.domain.notice.service.NoticeService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.service.NotificationService;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.service.ReportService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import user.enums.AlarmType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUseCase {
    private final UserService userService;
    private final BoardService boardService;
    private final ReportService reportService;
    private final InquiryService inquiryService;
    private final EnrollService enrollService;
    private final NoticeService noticeService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        User writer = userService.getUser(userId);

        Notice notice = Notice.createNotice(
                writer,
                command.getTitle(),
                command.getContent()
        );

        Notice savedNotice = noticeService.createNotice(notice);
        return NoticeCreateResponse.from(savedNotice);
    }

    @Transactional
    public InquiryAnswerResponse createInquiryAnswer(InquiryAnswerCommand command) {
        Inquiry inquiry = inquiryService.getInquiry(command.getInquiryId());

        inquiry.registerAnswer(command.getContent());
        Inquiry updatedInquiry = inquiryService.updateInquiry(inquiry);

        saveNotification(
                updatedInquiry.getUser(),
                null,
                "문의 답변이 도착했어요",
                updatedInquiry.getId()
        );

        publishInquiryAnswerEvent(
                updatedInquiry.getUser(),
                updatedInquiry,
                "문의 답변이 도착했어요",
                "관리자님이 회원님의 문의에 답변을 남겼어요. 확인해보세요!",
                "INQUIRY_ANSWER"
        );

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
        Report report = reportService.getReport(reportId);

        User reportedUser = report.getReportedUser();
        reportedUser.markAsReported();
        userService.updateUser(reportedUser);

        // TODO: 신고 처리 사유에 따른 추가 조치 (경고, 정지 등) 구현 필요
        report.process();
        reportService.updateReport(report);

        return ReportActionResponse.of(report.getId(), reportedUser.getId());
    }

    @Transactional
    public NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = noticeService.getNotice(noticeId);

        notice.updateNotice(
                command.getTitle(),
                command.getContent()
        );

        Notice updatedNotice = noticeService.updateNotice(notice);
        return NoticeDetailResponse.from(updatedNotice);
    }

    @Transactional
    public NoticeActionResponse deleteNotice(Long noticeId) {
        Notice notice = noticeService.getNotice(noticeId);
        noticeService.deleteNotice(notice);

        return NoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }

    private void saveNotification(User user, User sender, String title, Long referenceId) {
        Notification notification = Notification.createNotification(
                user,
                sender,
                null,
                title,
                AlarmType.INQUIRY_ANSWER,
                referenceId
        );
        notificationService.createNotification(notification);
    }

    private void publishInquiryAnswerEvent(User recipient, Inquiry inquiry, String title, String body, String type) {
        eventPublisher.publishEvent(
                new AdminInquiryAnswerNotificationEvent(
                        recipient,
                        inquiry,
                        title,
                        body,
                        type
                )
        );
    }
}
