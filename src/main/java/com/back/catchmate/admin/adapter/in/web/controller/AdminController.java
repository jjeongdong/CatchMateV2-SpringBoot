package com.back.catchmate.admin.adapter.in.web.controller;

import com.back.catchmate.admin.adapter.in.web.dto.request.InquiryAnswerRequest;
import com.back.catchmate.admin.adapter.in.web.dto.request.NoticeCreateRequest;
import com.back.catchmate.admin.adapter.in.web.dto.request.NoticeUpdateRequest;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.admin.application.port.in.AdminClientCommandUseCase;
import com.back.catchmate.admin.application.port.in.AdminClientQueryUseCase;
import com.back.catchmate.admin.application.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.application.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryAnswerResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeActionResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeCreateResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeUpdateResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportActionResponse;
import com.back.catchmate.common.response.PagedResponse;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminClientCommandUseCase adminClientCommandUseCase;
    private final AdminClientQueryUseCase adminClientQueryUseCase;

    @PostMapping("/notices")
    @Operation(summary = "공지사항 등록", description = "공지사항을 등록할 수 있습니다. (관리자 전용)")
    public ResponseEntity<AdminNoticeCreateResponse> createNotice(@AuthUser Long userId,
                                                                  @RequestBody @Valid NoticeCreateRequest request) {
        return ResponseEntity.ok(adminClientCommandUseCase.createNotice(userId, request.toCommand()));
    }

    @PostMapping("/inquiries/{inquiryId}/answer")
    @Operation(summary = "문의 답변 등록", description = "유저의 문의에 답변을 등록하고 상태를 '완료'로 변경합니다.")
    public ResponseEntity<AdminInquiryAnswerResponse> createInquiryAnswer(@PathVariable Long inquiryId,
                                                                          @RequestBody @Valid InquiryAnswerRequest request) {
        return ResponseEntity.ok(adminClientCommandUseCase.createInquiryAnswer(request.toCommand(inquiryId)));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminClientQueryUseCase.getDashboardStats());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 상세 정보 조회", description = "특정 유저의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminClientQueryUseCase.getUser(userId));
    }

    @GetMapping("/users")
    @Operation(summary = "관리자 유저 리스트 조회", description = "구단명을 파라미터로 받아 유저 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<AdminUserResponse>> getUserList(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)")
            @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getUserList(clubName, page, size));
    }

    @GetMapping("/boards/{boardId}")
    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 해당 게시글에 대한 신청자 목록을 조회합니다.")
    public ResponseEntity<AdminBoardDetailResponse> getBoardWithEnrollList(@PathVariable Long boardId) {
        return ResponseEntity.ok(adminClientQueryUseCase.getBoardWithEnrollList(boardId));
    }

    @GetMapping("/users/{userId}/boards")
    @Operation(summary = "유저 작성 게시글 조회", description = "특정 유저가 작성한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardListByUserId(@PathVariable Long userId,
                                                                                  @RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getBoardListByUserId(userId, page, size));
    }

    @GetMapping("/boards")
    @Operation(summary = "관리자 게시글 전체 조회", description = "전체 게시글을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardList(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getBoardList(page, size));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "관리자 신고 상세 조회", description = "특정 신고 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminClientQueryUseCase.getReport(reportId));
    }

    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 목록 조회", description = "전체 신고 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminReportResponse>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getReportList(page, size));
    }

    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiry(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminClientQueryUseCase.getInquiry(inquiryId));
    }

    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminInquiryResponse>> getInquiryList(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getInquiryList(page, size));
    }

    @GetMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    public ResponseEntity<AdminNoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(adminClientQueryUseCase.getNotice(noticeId));
    }

    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다. (page는 0부터 시작)")
    public ResponseEntity<PagedResponse<AdminNoticeResponse>> getNoticeList(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientQueryUseCase.getNoticeList(page, size));
    }

    @PostMapping("/reports/{reportId}/process")
    @Operation(summary = "신고 처리", description = "별도의 입력 정보 없이, 해당 신고 건을 처리하고 신고 당한 유저를 '신고됨(true)' 상태로 변경합니다.")
    public ResponseEntity<AdminReportActionResponse> updateReportProcess(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminClientCommandUseCase.updateReportProcess(reportId));
    }

    @PutMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 수정", description = "특정 공지사항을 수정합니다. (관리자 전용)")
    public ResponseEntity<AdminNoticeUpdateResponse> updateNotice(@AuthUser Long userId,
                                                                  @PathVariable Long noticeId,
                                                                  @RequestBody @Valid NoticeUpdateRequest request) {
        return ResponseEntity.ok(adminClientCommandUseCase.updateNotice(noticeId, request.toCommand()));
    }

    @DeleteMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다. (관리자 전용)")
    public ResponseEntity<AdminNoticeActionResponse> deleteNotice(@AuthUser Long userId,
                                                                  @PathVariable Long noticeId) {
        return ResponseEntity.ok(adminClientCommandUseCase.deleteNotice(noticeId));
    }
}
