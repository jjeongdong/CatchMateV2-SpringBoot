package com.back.catchmate.admin.service;

import com.back.catchmate.admin.dto.command.InquiryRegisterAnswerCommand;
import com.back.catchmate.admin.dto.request.NoticeCreateRequest;
import com.back.catchmate.admin.dto.request.NoticeUpdateRequest;
import com.back.catchmate.admin.dto.response.AdminAnswerDraftResponse;
import com.back.catchmate.admin.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.dto.response.AdminCorpusReindexResponse;
import com.back.catchmate.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.dto.response.AdminEnrollmentDetailResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryAnswerResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.dto.response.AdminNoticeActionResponse;
import com.back.catchmate.admin.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.admin.dto.response.AdminNoticeResponse;
import com.back.catchmate.admin.dto.response.AdminNoticeUpdateResponse;
import com.back.catchmate.admin.dto.response.AdminReportActionResponse;
import com.back.catchmate.admin.dto.response.AdminReportDetailResponse;
import com.back.catchmate.admin.dto.response.AdminReportResponse;
import com.back.catchmate.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.dto.response.AdminUserResponse;
import com.back.catchmate.admin.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.admin.event.NoticeCreatedEvent;
import com.back.catchmate.board.dto.response.BoardAdminView;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.dto.response.EnrollSummary;
import com.back.catchmate.enroll.service.EnrollQueryService;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.inquiry.dto.response.InquirySummary;
import com.back.catchmate.inquiry.service.InquiryService;
import com.back.catchmate.notice.dto.response.NoticeCreateResponse;
import com.back.catchmate.notice.dto.response.NoticeSummary;
import com.back.catchmate.notice.service.NoticeService;
import com.back.catchmate.report.dto.response.ReportSummary;
import com.back.catchmate.report.service.ReportService;
import com.back.catchmate.user.application.UserCommandService;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {
    private final ClubQueryApi clubQueryApi;
    private final GameQueryApi gameQueryApi;
    private final UserQueryApi userQueryApi;
    private final UserCommandService userCommandService;
    private final BoardService boardService;
    private final NoticeService noticeService;
    private final EnrollQueryService enrollQueryService;
    private final ReportService reportService;
    private final InquiryService inquiryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.of(
                userQueryApi.count(),
                AdminDashboardResponse.GenderRatio.of(userQueryApi.countByGender('M'), userQueryApi.countByGender('F')),
                boardService.getTotalBoardCount(),
                resolveUserCountByClubName(),
                userQueryApi.countByWatchStyles(),
                reportService.getTotalReportCount(),
                reportService.getPendingReportCount(),
                inquiryService.getTotalInquiryCount(),
                inquiryService.getWaitingInquiryCount());
    }

    private Map<String, Long> resolveUserCountByClubName() {
        Map<Long, Long> countByClubId = userQueryApi.countByClubIds();
        if (countByClubId.isEmpty()) return Map.of();
        Map<Long, ClubInfo> clubById = clubQueryApi.getInfos(List.copyOf(countByClubId.keySet()));
        return countByClubId.entrySet().stream()
                .filter(e -> clubById.get(e.getKey()) != null)
                .collect(Collectors.toMap(e -> clubById.get(e.getKey()).name(), Map.Entry::getValue));
    }

    public AdminUserDetailResponse getUser(Long userId) {
        UserInfo user = userQueryApi.getInfo(userId);
        ClubInfo club = user.clubId() != null ? clubQueryApi.getInfo(user.clubId()) : null;
        return AdminUserDetailResponse.from(user, club != null ? club.name() : null);
    }

    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Long clubId = null;
        if (clubName != null && !clubName.isBlank()) {
            Optional<ClubInfo> club = clubQueryApi.findInfoByName(clubName);
            if (club.isEmpty()) {
                return new PagedResponse<>(Page.empty(pageable), List.of());
            }
            clubId = club.get().clubId();
        }

        Page<UserInfo> userPage = new PageImpl<>(
                userQueryApi.getInfosByClubId(clubId, page, size), pageable, userQueryApi.countByClubId(clubId));

        Map<Long, ClubInfo> clubById = resolveUserClubs(userPage.getContent());

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(u -> AdminUserResponse.from(
                        u,
                        u.clubId() != null && clubById.get(u.clubId()) != null
                                ? clubById.get(u.clubId()).name()
                                : null))
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    public AdminBoardDetailResponse getBoardWithEnrollList(Long boardId) {
        BoardSummary board = boardService.getCompletedBoardSummary(boardId);
        List<EnrollSummary> enrolls = enrollQueryService.getEnrollListByBoardIds(Collections.singletonList(boardId));

        List<Long> enrollUserIds =
                enrolls.stream().map(EnrollSummary::userId).distinct().toList();
        Map<Long, UserInfo> enrollUserById = enrollUserIds.isEmpty() ? Map.of() : userQueryApi.getInfos(enrollUserIds);
        Map<Long, ClubInfo> enrollUserClubById = resolveUserClubs(enrollUserById.values());

        List<AdminEnrollmentDetailResponse> enrollmentInfos = enrolls.stream()
                .map(enroll -> {
                    UserInfo u = enrollUserById.get(enroll.userId());
                    ClubInfo c = u != null && u.clubId() != null ? enrollUserClubById.get(u.clubId()) : null;
                    return AdminEnrollmentDetailResponse.from(enroll, u, c != null ? c.name() : null);
                })
                .toList();

        UserInfo writer = board.userId() != null ? userQueryApi.getInfo(board.userId()) : null;
        GameInfo game = board.gameId() != null ? gameQueryApi.getInfo(board.gameId()) : null;

        return AdminBoardDetailResponse.from(board, writer, game, enrollmentInfos);
    }

    public PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BoardAdminView> boardPage = boardService.getBoardAdminViewsByUserId(userId, pageable);

        List<AdminBoardResponse> responses =
                boardPage.getContent().stream().map(AdminBoardResponse::from).toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminBoardResponse> getBoardList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BoardAdminView> boardPage = boardService.getBoardAdminViews(pageable);

        List<AdminBoardResponse> responses =
                boardPage.getContent().stream().map(AdminBoardResponse::from).toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public AdminReportDetailResponse getReport(Long reportId) {
        ReportSummary report = reportService.getReportSummary(reportId);
        UserInfo reporter = userQueryApi.getInfo(report.reporterId());
        UserInfo reportedUser = userQueryApi.getInfo(report.reportedUserId());
        return AdminReportDetailResponse.from(report, reporter, reportedUser);
    }

    public PagedResponse<AdminReportResponse> getReportList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportSummary> reportPage = reportService.getReportSummaries(pageable);

        Map<Long, UserInfo> reporterById = userQueryApi.getInfos(reportPage.getContent().stream()
                .map(ReportSummary::reporterId)
                .distinct()
                .toList());

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(r -> AdminReportResponse.from(r, reporterById.get(r.reporterId())))
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
        InquirySummary inquiry = inquiryService.getInquirySummary(inquiryId);
        UserInfo user = userQueryApi.getInfo(inquiry.userId());
        return AdminInquiryDetailResponse.from(inquiry, user);
    }

    public PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InquirySummary> inquiryPage = inquiryService.getInquirySummaries(pageable);

        Map<Long, UserInfo> userById = userQueryApi.getInfos(inquiryPage.getContent().stream()
                .map(InquirySummary::userId)
                .distinct()
                .toList());

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(i -> AdminInquiryResponse.from(i, userById.get(i.userId())))
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminAnswerDraftResponse getInquiryAnswerDraft(Long inquiryId) {
        return AdminAnswerDraftResponse.from(inquiryService.draftAnswer(inquiryId));
    }

    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        NoticeSummary notice = noticeService.getNoticeSummary(noticeId);
        UserInfo writer = userQueryApi.getInfo(notice.writerId());
        return AdminNoticeDetailResponse.from(notice, writer.nickName());
    }

    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NoticeSummary> noticePage = noticeService.getNoticeSummaries(pageable);

        Map<Long, String> writerNicknameById = userQueryApi
                .getInfos(noticePage.getContent().stream()
                        .map(NoticeSummary::writerId)
                        .distinct()
                        .toList())
                .values()
                .stream()
                .collect(Collectors.toMap(UserInfo::userId, UserInfo::nickName));

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> AdminNoticeResponse.from(n, writerNicknameById.getOrDefault(n.writerId(), "")))
                .toList();

        return new PagedResponse<>(noticePage, responses);
    }

    private Map<Long, ClubInfo> resolveUserClubs(Collection<UserInfo> users) {
        List<Long> clubIds = users.stream()
                .map(UserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) return Map.of();
        return clubQueryApi.getInfos(clubIds);
    }

    @Transactional
    public AdminNoticeCreateResponse createNotice(Long userId, NoticeCreateRequest request) {
        NoticeCreateResponse created = noticeService.createNotice(userId, request.title(), request.content());
        AdminNoticeCreateResponse response = AdminNoticeCreateResponse.from(created);

        applicationEventPublisher.publishEvent(NoticeCreatedEvent.of(response.noticeId(), request.title()));

        return response;
    }

    @Transactional
    public AdminInquiryAnswerResponse createInquiryAnswer(InquiryRegisterAnswerCommand command) {
        inquiryService.registerAnswer(command.inquiryId(), command.content());

        InquirySummary updatedInquiry = inquiryService.getInquirySummary(command.inquiryId());
        applicationEventPublisher.publishEvent(
                InquiryAnswerRegisteredEvent.of(updatedInquiry.inquiryId(), updatedInquiry.userId()));

        return AdminInquiryAnswerResponse.of(updatedInquiry.inquiryId(), updatedInquiry.userId());
    }

    @Transactional
    public AdminCorpusReindexResponse reindexInquiryCorpus() {
        return AdminCorpusReindexResponse.of(inquiryService.reindex());
    }

    @Transactional
    public AdminReportActionResponse updateReportProcess(Long reportId) {
        ReportSummary report = reportService.getReportSummary(reportId);
        Long reportedUserId = report.reportedUserId();

        userCommandService.markUserAsReported(reportedUserId);
        reportService.processReport(reportId);

        return AdminReportActionResponse.of(reportId, reportedUserId);
    }

    @Transactional
    public AdminNoticeUpdateResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        noticeService.updateNotice(noticeId, request.title(), request.content());

        NoticeSummary updatedNotice = noticeService.getNoticeSummary(noticeId);
        UserInfo writer = userQueryApi.getInfo(updatedNotice.writerId());
        return AdminNoticeUpdateResponse.from(updatedNotice, writer.nickName());
    }

    @Transactional
    public AdminNoticeActionResponse deleteNotice(Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return AdminNoticeActionResponse.of(noticeId, "공지사항이 삭제되었습니다.");
    }
}
