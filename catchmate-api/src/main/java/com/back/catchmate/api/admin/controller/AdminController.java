package com.back.catchmate.api.admin.controller;

import com.back.catchmate.api.admin.dto.request.InquiryAnswerRequest;
import com.back.catchmate.api.admin.dto.request.NoticeCreateRequest;
import com.back.catchmate.api.admin.dto.request.NoticeUpdateRequest;
import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.orchestration.admin.AdminOrchestrator;
import com.back.catchmate.orchestration.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.orchestration.admin.dto.response.AdminDashboardResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[관리자] 관리자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminOrchestrator adminOrchestrator;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/notices")
    @Operation(summary = "공지사항 등록", description = "공지사항을 등록할 수 있습니다. (관리자 전용)")
    public ResponseEntity<NoticeCreateResponse> createNotice(@AuthUser Long userId,
                                                             @RequestBody @Valid NoticeCreateRequest request) {
        return ResponseEntity.ok(adminOrchestrator.createNotice(userId, request.toCommand()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inquiries/{inquiryId}/answer")
    @Operation(summary = "문의 답변 등록", description = "유저의 문의에 답변을 등록하고 상태를 '완료'로 변경합니다.")
    public ResponseEntity<InquiryAnswerResponse> createInquiryAnswer(@PathVariable Long inquiryId,
                                                                     @RequestBody @Valid InquiryAnswerRequest request) {
        return ResponseEntity.ok(adminOrchestrator.createInquiryAnswer(request.toCommand(inquiryId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/stats")
    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminOrchestrator.getDashboardStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 상세 정보 조회", description = "특정 유저의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminOrchestrator.getUser(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    @Operation(summary = "관리자 유저 리스트 조회", description = "구단명을 파라미터로 받아 유저 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<AdminUserResponse>> getUserList(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)")
            @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getUserList(clubName, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards/{boardId}")
    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 해당 게시글에 대한 신청자 목록을 조회합니다.")
    public ResponseEntity<AdminBoardDetailWithEnrollResponse> getBoardWithEnrollList(@PathVariable Long boardId) {
        return ResponseEntity.ok(adminOrchestrator.getBoardWithEnrollList(boardId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/boards")
    @Operation(summary = "유저 작성 게시글 조회", description = "특정 유저가 작성한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardListByUserId(@PathVariable Long userId,
                                                                                  @RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getBoardListByUserId(userId, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards")
    @Operation(summary = "관리자 게시글 전체 조회", description = "전체 게시글을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardList(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getBoardList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "관리자 신고 상세 조회", description = "특정 신고 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminOrchestrator.getReport(reportId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 목록 조회", description = "전체 신고 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminReportResponse>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getReportList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiry(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminOrchestrator.getInquiry(inquiryId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminInquiryResponse>> getInquiryList(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getInquiryList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    public ResponseEntity<AdminNoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(adminOrchestrator.getNotice(noticeId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다. (page는 0부터 시작)")
    public ResponseEntity<PagedResponse<AdminNoticeResponse>> getNoticeList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminOrchestrator.getNoticeList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reports/{reportId}/process")
    @Operation(summary = "신고 처리", description = "별도의 입력 정보 없이, 해당 신고 건을 처리하고 신고 당한 유저를 '신고됨(true)' 상태로 변경합니다.")
    public ResponseEntity<ReportActionResponse> updateReportProcess(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminOrchestrator.updateReportProcess(reportId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 수정", description = "특정 공지사항을 수정합니다. (관리자 전용)")
    public ResponseEntity<NoticeDetailResponse> updateNotice(@AuthUser Long userId,
                                                             @PathVariable Long noticeId,
                                                             @RequestBody @Valid NoticeUpdateRequest request) {
        return ResponseEntity.ok(adminOrchestrator.updateNotice(noticeId, request.toCommand()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다. (관리자 전용)")
    public ResponseEntity<NoticeActionResponse> deleteNotice(@AuthUser Long userId,
                                                             @PathVariable Long noticeId) {
        return ResponseEntity.ok(adminOrchestrator.deleteNotice(noticeId));
    }
}
