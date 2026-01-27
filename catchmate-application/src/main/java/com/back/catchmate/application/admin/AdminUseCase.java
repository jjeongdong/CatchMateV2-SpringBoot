package com.back.catchmate.application.admin;

import com.back.catchmate.application.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.application.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.application.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.application.admin.dto.response.AdminEnrollmentResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.service.EnrollService;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.service.InquiryService;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.service.ReportService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUseCase {
    private final UserService userService;
    private final BoardService boardService;
    private final ReportService reportService;
    private final InquiryService inquiryService;
    private final EnrollService enrollService;

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

    // 유저 상세 정보 조회
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userService.getUserById(userId);
        return AdminUserDetailResponse.from(user);
    }

    // 유저 작성 게시글 리스트 조회
    public PagedResponse<AdminBoardResponse> getUserBoards(Long userId, int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardsByUserId(userId, domainPageable);

        List<AdminBoardResponse> responses = boardPage.getContent().stream()
                .map(AdminBoardResponse::from)
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public PagedResponse<AdminBoardResponse> getAllBoards(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(domainPageable);

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

    public PagedResponse<AdminReportResponse> getAllReports(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Report> reportPage = reportService.getAllReports(domainPageable);

        List<AdminReportResponse> responses = reportPage.getContent().stream()
                .map(AdminReportResponse::from)
                .toList();

        return new PagedResponse<>(reportPage, responses);
    }

    public PagedResponse<AdminInquiryResponse> getAllInquiries(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Inquiry> inquiryPage = inquiryService.getAllInquiries(domainPageable);

        List<AdminInquiryResponse> responses = inquiryPage.getContent().stream()
                .map(AdminInquiryResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public AdminReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportService.getReport(reportId);
        return AdminReportDetailResponse.from(report);
    }

    // 4. 문의 상세 조회
    public AdminInquiryDetailResponse getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);
        return AdminInquiryDetailResponse.from(inquiry);
    }
}
