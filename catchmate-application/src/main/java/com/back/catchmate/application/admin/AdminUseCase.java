package com.back.catchmate.application.admin;

import com.back.catchmate.application.admin.dto.command.InquiryAnswerCommand;
import com.back.catchmate.application.admin.dto.command.NoticeCreateCommand;
import com.back.catchmate.application.admin.dto.command.NoticeUpdateCommand;
import com.back.catchmate.application.admin.dto.command.ReportActionCommand;
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
import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.notification.service.NotificationService;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.service.ReportService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import user.enums.AlarmType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.builder()
                .totalUserCount(userService.getTotalUserCount())
                .genderRatio(new AdminDashboardResponse.GenderRatio(
                        userService.getUserCountByGender('M'),
                        userService.getUserCountByGender('F')
                ))
                .totalBoardCount(boardService.getTotalBoardCount())
                .userCountByClub(userService.getUserCountByClub())
                .userCountByWatchStyle(userService.getUserCountByWatchStyle())
                .totalReportCount(reportService.getTotalReportCount())
                .totalInquiryCount(inquiryService.getTotalInquiryCount())
                .build();
    }

    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<User> userPage = userService.getUsersByClub(clubName, domainPageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userService.getUserById(userId);
        return AdminUserDetailResponse.from(user);
    }

    public PagedResponse<AdminBoardResponse> getUserBoards(Long userId, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardListByUserId(userId, domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public AdminBoardDetailWithEnrollResponse getBoardDetailWithEnrollments(Long boardId) {
        // 게시글 정보 조회
        Board board = boardService.getCompletedBoard(boardId);

        // 해당 게시글의 신청 내역 조회
        List<Enroll> enrolls = enrollService.getEnrollsByBoardIds(Collections.singletonList(boardId));

        // DTO 변환
        List<AdminEnrollmentResponse> enrollmentInfos = enrolls.stream()
                .map(AdminEnrollmentResponse::from)
                .toList();

        return AdminBoardDetailWithEnrollResponse.of(board, enrollmentInfos);
    }

    public PagedResponse<AdminBoardResponse> getAllBoards(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminReportResponse> getAllReports(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Report> reportPage = reportService.getAllReports(domainPageable);

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(AdminReportResponse::from)
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public AdminReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportService.getReport(reportId);
        return AdminReportDetailResponse.from(report);
    }

    @Transactional
    public ReportActionResponse processReport(ReportActionCommand command) {
        // 1. 신고 내역 조회
        Report report = reportService.getReport(command.getReportId());

        // 2. 신고 당한 유저 조회 및 상태 변경
        User reportedUser = report.getReportedUser();
        reportedUser.markAsReported();
        userService.updateUser(reportedUser);

        // 3. 신고 내역 상태 업데이트
        report.process();
        reportService.update(report);

        // TODO: 신고 처리 절차 추후 논의 후 추가 예정
        return ReportActionResponse.of(report.getId(), reportedUser.getId());
    }

    public PagedResponse<AdminInquiryResponse> getAllInquiries(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Inquiry> inquiryPage = inquiryService.getAllInquiries(domainPageable);

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(AdminInquiryResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminInquiryDetailResponse getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);
        return AdminInquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public InquiryAnswerResponse answerInquiry(InquiryAnswerCommand command) {
        // 1. 문의 조회
        Inquiry inquiry = inquiryService.getInquiry(command.getInquiryId());

        // 2. 답변 등록
        inquiry.registerAnswer(command.getContent());
        inquiryService.update(inquiry);

        // 3. 알림 저장 및 푸시 알림 전송
        saveNotification(
                inquiry.getUser(),
                null,
                "문의 답변이 도착했어요",
                AlarmType.INQUIRY_ANSWER,
                inquiry.getId()
        );

        sendInquiryNotification(
                inquiry.getUser(),
                inquiry,
                "문의 답변이 도착했어요",
                "관리자님이 회원님의 문의에 답변을 남겼어요. 확인해보세요!",
                "INQUIRY_ANSWER"
        );

        return InquiryAnswerResponse.of(inquiry.getId(), inquiry.getUser().getId());
    }

    private void saveNotification(User user, User sender, String title, AlarmType type, Long referenceId) {
        Notification notification = Notification.createNotification(
                user,
                sender,
                title,
                type,
                referenceId
        );
        notificationService.createNotification(notification);
    }

    private void sendInquiryNotification(User recipient, Inquiry inquiry, String title, String body, String type) {
        if (recipient.getFcmToken() != null && recipient.getEventAlarm() == 'Y') {
            Map<String, String> data = Map.of(
                    "type", type,
                    "inquiryId", inquiry.getId().toString()
            );

            notificationSender.sendNotification(recipient.getFcmToken(), title, body, data);
        }
    }

    @Transactional
    public NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command) {
        // 작성자(관리자) 정보 조회
        User writer = userService.getUserById(userId);

        Notice notice = Notice.createNotice(
                writer,
                command.getTitle(),
                command.getContent()
        );

        Notice savedNotice = noticeService.createNotice(notice);
        return NoticeCreateResponse.from(savedNotice.getId());
    }

    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);

        DomainPage<Notice> noticePage = noticeService.getAllNotices(domainPageable);

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(AdminNoticeResponse::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    public AdminNoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeService.getNotice(noticeId);
        return AdminNoticeDetailResponse.from(notice);
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
}

