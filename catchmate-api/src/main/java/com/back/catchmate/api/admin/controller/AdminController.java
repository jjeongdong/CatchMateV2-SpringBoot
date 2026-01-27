package com.back.catchmate.api.admin.controller;

import com.back.catchmate.application.admin.AdminUseCase;
import com.back.catchmate.application.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.application.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.application.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminReportResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.application.admin.dto.response.AdminUserResponse;
import com.back.catchmate.application.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[관리자] 관리자 관련 PI")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final AdminUseCase adminUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/stats")
    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminUseCase.getDashboardStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    @Operation(summary = "관리자 유저 리스트 조회", description = "구단명을 파라미터로 받아 유저 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<AdminUserResponse>> getUserList(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)")
            @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUseCase.getUserList(clubName, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 상세 정보 조회", description = "특정 유저의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUseCase.getUserDetail(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/boards")
    @Operation(summary = "유저 작성 게시글 조회", description = "특정 유저가 작성한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getUserBoards(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUseCase.getUserBoards(userId, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards")
    @Operation(summary = "관리자 게시글 전체 조회", description = "전체 게시글을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getAllBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUseCase.getAllBoards(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards/{boardId}")
    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 해당 게시글에 대한 신청자 목록을 조회합니다.")
    public ResponseEntity<AdminBoardDetailWithEnrollResponse> getBoardDetail(
            @PathVariable Long boardId
    ) {
        return ResponseEntity.ok(adminUseCase.getBoardDetailWithEnrollments(boardId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 목록 조회", description = "전체 신고 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminReportResponse>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUseCase.getAllReports(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "관리자 신고 상세 조회", description = "특정 신고 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminReportDetailResponse> getReportDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminUseCase.getReportDetail(reportId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminInquiryResponse>> getAllInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUseCase.getAllInquiries(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiryDetail(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminUseCase.getInquiryDetail(inquiryId));
    }
}
