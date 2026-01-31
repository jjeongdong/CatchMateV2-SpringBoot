package com.back.catchmate.api.admin.controller;

import com.back.catchmate.api.admin.dto.request.InquiryAnswerRequest;
import com.back.catchmate.api.admin.dto.request.NoticeCreateRequest;
import com.back.catchmate.api.admin.dto.request.NoticeUpdateRequest;
import com.back.catchmate.application.admin.AdminUseCase;
import com.back.catchmate.application.admin.dto.response.AdminBoardDetailWithEnrollResponse;
import com.back.catchmate.application.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.application.admin.dto.response.AdminDashboardResponse;
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
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[관리자] 관리자 관련 PI")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminUseCase adminUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/notices")
    @Operation(summary = "공지사항 등록", description = "공지사항을 등록할 수 있습니다. (관리자 전용)")
    public ResponseEntity<NoticeCreateResponse> createNotice(@AuthUser Long userId,
                                                             @RequestBody @Valid NoticeCreateRequest request) {
        return ResponseEntity.ok(adminUseCase.createNotice(userId, request.toCommand()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inquiries/{inquiryId}/answer")
    @Operation(summary = "문의 답변 등록", description = "유저의 문의에 답변을 등록하고 상태를 '완료'로 변경합니다.")
    public ResponseEntity<InquiryAnswerResponse> createInquiryAnswer(@PathVariable Long inquiryId,
                                                                     @RequestBody @Valid InquiryAnswerRequest request) {
        return ResponseEntity.ok(adminUseCase.createInquiryAnswer(request.toCommand(inquiryId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/stats")
    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminUseCase.getDashboardStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 상세 정보 조회", description = "특정 유저의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUseCase.getUser(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    @Operation(summary = "관리자 유저 리스트 조회", description = "구단명을 파라미터로 받아 유저 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<AdminUserResponse>> getUserList(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)")
            @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getUserList(clubName, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards/{boardId}")
    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 해당 게시글에 대한 신청자 목록을 조회합니다.")
    public ResponseEntity<AdminBoardDetailWithEnrollResponse> getBoardWithEnrollList(@PathVariable Long boardId) {
        return ResponseEntity.ok(adminUseCase.getBoardWithEnrollList(boardId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/boards")
    @Operation(summary = "유저 작성 게시글 조회", description = "특정 유저가 작성한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardListByUserId(@PathVariable Long userId,
                                                                                  @RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getBoardListByUserId(userId, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/boards")
    @Operation(summary = "관리자 게시글 전체 조회", description = "전체 게시글을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardList(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getBoardList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "관리자 신고 상세 조회", description = "특정 신고 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminUseCase.getReport(reportId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 목록 조회", description = "전체 신고 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminReportResponse>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getReportList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiry(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminUseCase.getInquiry(inquiryId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminInquiryResponse>> getInquiryList(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getInquiryList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    public ResponseEntity<AdminNoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(adminUseCase.getNotice(noticeId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다. (page는 0부터 시작)")
    public ResponseEntity<PagedResponse<AdminNoticeResponse>> getNoticeList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUseCase.getNoticeList(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reports/{reportId}/process")
    @Operation(summary = "신고 처리", description = "별도의 입력 정보 없이, 해당 신고 건을 처리하고 신고 당한 유저를 '신고됨(true)' 상태로 변경합니다.")
    public ResponseEntity<ReportActionResponse> updateReportProcess(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminUseCase.updateReportProcess(reportId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 수정", description = "특정 공지사항을 수정합니다. (관리자 전용)")
    public ResponseEntity<NoticeDetailResponse> updateNotice(@AuthUser Long userId,
                                                             @PathVariable Long noticeId,
                                                             @RequestBody @Valid NoticeUpdateRequest request) {
        return ResponseEntity.ok(adminUseCase.updateNotice(noticeId, request.toCommand()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다. (관리자 전용)")
    public ResponseEntity<NoticeActionResponse> deleteNotice(@AuthUser Long userId,
                                                             @PathVariable Long noticeId) {
        return ResponseEntity.ok(adminUseCase.deleteNotice(noticeId));
    }
}
