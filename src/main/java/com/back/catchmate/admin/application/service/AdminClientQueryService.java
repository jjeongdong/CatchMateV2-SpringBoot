package com.back.catchmate.admin.application.service;

import com.back.catchmate.admin.application.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.application.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.application.dto.response.AdminEnrollmentDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserResponse;
import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.port.in.AdminClientQueryUseCase;
import com.back.catchmate.admin.application.port.out.dto.AdminBoardInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminClubInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminEnrollInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminGameInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;
import com.back.catchmate.admin.application.port.out.external.BoardFetchPort;
import com.back.catchmate.admin.application.port.out.external.ClubFetchPort;
import com.back.catchmate.admin.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.admin.application.port.out.external.GameFetchPort;
import com.back.catchmate.admin.application.port.out.external.InquiryFetchPort;
import com.back.catchmate.admin.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.admin.application.port.out.external.ReportFetchPort;
import com.back.catchmate.admin.application.port.out.external.UserFetchPort;
import com.back.catchmate.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminClientQueryService implements AdminClientQueryUseCase {
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;
    private final NoticeFetchPort noticeFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final ReportFetchPort reportFetchPort;
    private final InquiryFetchPort inquiryFetchPort;

    @Override
    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.of(
                userFetchPort.getTotalUserCount(),
                AdminDashboardResponse.GenderRatio.of(
                        userFetchPort.getUserCountByGender('M'),
                        userFetchPort.getUserCountByGender('F')
                ),
                boardFetchPort.getTotalBoardCount(),
                resolveUserCountByClubName(),
                userFetchPort.getUserCountByWatchStyle(),
                reportFetchPort.getTotalReportCount(),
                reportFetchPort.getPendingReportCount(),
                inquiryFetchPort.getTotalInquiryCount(),
                inquiryFetchPort.getWaitingInquiryCount()
        );
    }

    private Map<String, Long> resolveUserCountByClubName() {
        Map<Long, Long> countByClubId = userFetchPort.getUserCountByClubId();
        if (countByClubId.isEmpty()) return Map.of();
        Map<Long, AdminClubInfo> clubById = clubFetchPort.getClubs(List.copyOf(countByClubId.keySet())).stream()
                .collect(Collectors.toMap(AdminClubInfo::clubId, Function.identity()));
        return countByClubId.entrySet().stream()
                .filter(e -> clubById.get(e.getKey()) != null)
                .collect(Collectors.toMap(e -> clubById.get(e.getKey()).name(), Map.Entry::getValue));
    }

    @Override
    public AdminUserDetailResponse getUser(Long userId) {
        AdminUserInfo user = userFetchPort.getUser(userId);
        AdminClubInfo club = user.clubId() != null ? clubFetchPort.getClub(user.clubId()) : null;
        return AdminUserDetailResponse.from(user, club != null ? club.name() : null);
    }

    @Override
    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Long clubId = null;
        if (clubName != null && !clubName.isBlank()) {
            Optional<AdminClubInfo> club = clubFetchPort.findClubByName(clubName);
            if (club.isEmpty()) {
                return new PagedResponse<>(Page.empty(pageable), List.of());
            }
            clubId = club.get().clubId();
        }

        Page<AdminUserInfo> userPage = userFetchPort.getUsersByClubId(clubId, pageable);

        Map<Long, AdminClubInfo> clubById = resolveUserClubs(userPage.getContent());

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(u -> AdminUserResponse.from(u, u.clubId() != null && clubById.get(u.clubId()) != null ? clubById.get(u.clubId()).name() : null))
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    @Override
    public AdminBoardDetailResponse getBoardWithEnrollList(Long boardId) {
        AdminBoardInfo board = boardFetchPort.getCompletedBoard(boardId);
        List<AdminEnrollInfo> enrolls = enrollFetchPort.getEnrollListByBoardIds(Collections.singletonList(boardId));

        List<Long> enrollUserIds = enrolls.stream()
                .map(AdminEnrollInfo::userId)
                .distinct()
                .toList();
        Map<Long, AdminUserInfo> enrollUserById = enrollUserIds.isEmpty()
                ? Map.of()
                : userFetchPort.getUsers(enrollUserIds).stream()
                .collect(Collectors.toMap(AdminUserInfo::userId, Function.identity()));
        Map<Long, AdminClubInfo> enrollUserClubById = resolveUserClubs(enrollUserById.values());

        List<AdminEnrollmentDetailResponse> enrollmentInfos = enrolls.stream()
                .map(enroll -> {
                    AdminUserInfo u = enrollUserById.get(enroll.userId());
                    AdminClubInfo c = u != null && u.clubId() != null ? enrollUserClubById.get(u.clubId()) : null;
                    return AdminEnrollmentDetailResponse.from(enroll, u, c != null ? c.name() : null);
                })
                .toList();

        AdminUserInfo writer = board.userId() != null ? userFetchPort.getUser(board.userId()) : null;
        AdminGameInfo game = board.gameId() != null ? gameFetchPort.getGame(board.gameId()) : null;

        return AdminBoardDetailResponse.from(board, writer, game, enrollmentInfos);
    }

    @Override
    public PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminBoardInfo> boardPage = boardFetchPort.getBoardListByUserId(userId, pageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    @Override
    public PagedResponse<AdminBoardResponse> getBoardList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminBoardInfo> boardPage = boardFetchPort.getBoardList(pageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    @Override
    public AdminReportDetailResponse getReport(Long reportId) {
        AdminReportInfo report = reportFetchPort.getReport(reportId);
        AdminUserInfo reporter = userFetchPort.getUser(report.reporterId());
        AdminUserInfo reportedUser = userFetchPort.getUser(report.reportedUserId());
        return AdminReportDetailResponse.from(report, reporter, reportedUser);
    }

    @Override
    public PagedResponse<AdminReportResponse> getReportList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminReportInfo> reportPage = reportFetchPort.getReportList(pageable);

        Map<Long, AdminUserInfo> reporterById = userFetchPort.getUsers(
                reportPage.getContent().stream().map(AdminReportInfo::reporterId).distinct().toList()
        ).stream().collect(Collectors.toMap(AdminUserInfo::userId, Function.identity()));

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(r -> AdminReportResponse.from(r, reporterById.get(r.reporterId())))
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    @Override
    public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
        AdminInquiryInfo inquiry = inquiryFetchPort.getInquiry(inquiryId);
        AdminUserInfo user = userFetchPort.getUser(inquiry.userId());
        return AdminInquiryDetailResponse.from(inquiry, user);
    }

    @Override
    public PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminInquiryInfo> inquiryPage = inquiryFetchPort.getInquiryList(pageable);

        Map<Long, AdminUserInfo> userById = userFetchPort.getUsers(
                inquiryPage.getContent().stream().map(AdminInquiryInfo::userId).distinct().toList()
        ).stream().collect(Collectors.toMap(AdminUserInfo::userId, Function.identity()));

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(i -> AdminInquiryResponse.from(i, userById.get(i.userId())))
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    @Override
    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        AdminNoticeInfo notice = noticeFetchPort.getNotice(noticeId);
        AdminUserInfo writer = userFetchPort.getUser(notice.writerId());
        return AdminNoticeDetailResponse.from(notice, writer.nickName());
    }

    @Override
    public PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminNoticeInfo> noticePage = noticeFetchPort.getNoticeList(pageable);

        Map<Long, String> writerNicknameById = userFetchPort.getUsers(
                noticePage.getContent().stream().map(AdminNoticeInfo::writerId).distinct().toList()
        ).stream().collect(Collectors.toMap(AdminUserInfo::userId, AdminUserInfo::nickName));

        List<AdminNoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> AdminNoticeResponse.from(n, writerNicknameById.getOrDefault(n.writerId(), "")))
                .toList();

        return new PagedResponse<>(noticePage, responses);
    }

    private Map<Long, AdminClubInfo> resolveUserClubs(Collection<AdminUserInfo> users) {
        List<Long> clubIds = users.stream()
                .map(AdminUserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) return Map.of();
        return clubFetchPort.getClubs(clubIds).stream()
                .collect(Collectors.toMap(AdminClubInfo::clubId, Function.identity()));
    }
}
