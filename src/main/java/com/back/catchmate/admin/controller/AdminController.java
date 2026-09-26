package com.back.catchmate.admin.controller;

import com.back.catchmate.admin.dto.request.InquiryAnswerRequest;
import com.back.catchmate.admin.dto.response.AdminAnswerDraftResponse;
import com.back.catchmate.admin.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.dto.response.AdminCorpusReindexResponse;
import com.back.catchmate.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryAnswerResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.dto.response.AdminUserResponse;
import com.back.catchmate.admin.service.AdminService;
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
    private final AdminService adminService;

    @PostMapping("/inquiries/{inquiryId}/answer")
    @Operation(summary = "문의 답변 등록", description = "유저의 문의에 답변을 등록하고 상태를 '완료'로 변경합니다.")
    public ResponseEntity<AdminInquiryAnswerResponse> createInquiryAnswer(
            @PathVariable Long inquiryId, @RequestBody @Valid InquiryAnswerRequest request) {
        return ResponseEntity.ok(adminService.createInquiryAnswer(request.toCommand(inquiryId)));
    }

    @PostMapping("/inquiries/{inquiryId}/answer-draft")
    @Operation(summary = "문의 답변 초안 생성(AI)", description = "공지·과거 답변을 근거로 RAG 답변 초안을 생성합니다. 근거가 없으면 직접 작성을 안내합니다.")
    public ResponseEntity<AdminAnswerDraftResponse> getInquiryAnswerDraft(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminService.getInquiryAnswerDraft(inquiryId));
    }

    @PostMapping("/inquiries/reindex")
    @Operation(summary = "RAG 코퍼스 수동 재색인", description = "공지·답변완료 문의를 벡터 스토어에 즉시 재적재합니다. (테스트/운영용)")
    public ResponseEntity<AdminCorpusReindexResponse> reindexInquiryCorpus() {
        return ResponseEntity.ok(adminService.reindexInquiryCorpus());
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "관리자 대시보드 통계 조회", description = "관리자 대시보드에 필요한 통계 정보를 조회합니다.")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 상세 정보 조회", description = "특정 유저의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @GetMapping("/users")
    @Operation(summary = "관리자 유저 리스트 조회", description = "구단명을 파라미터로 받아 유저 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<AdminUserResponse>> getUserList(
            @Parameter(description = "구단명 (옵션, 비워두면 전체 조회)") @RequestParam(required = false) String clubName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getUserList(clubName, page, size));
    }

    @GetMapping("/boards/{boardId}")
    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 해당 게시글에 대한 신청자 목록을 조회합니다.")
    public ResponseEntity<AdminBoardDetailResponse> getBoardWithEnrollList(@PathVariable Long boardId) {
        return ResponseEntity.ok(adminService.getBoardWithEnrollList(boardId));
    }

    @GetMapping("/users/{userId}/boards")
    @Operation(summary = "유저 작성 게시글 조회", description = "특정 유저가 작성한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardListByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getBoardListByUserId(userId, page, size));
    }

    @GetMapping("/boards")
    @Operation(summary = "관리자 게시글 전체 조회", description = "전체 게시글을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminBoardResponse>> getBoardList(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getBoardList(page, size));
    }

    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "특정 문의 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiry(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(adminService.getInquiry(inquiryId));
    }

    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "전체 1:1 문의 내역을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<AdminInquiryResponse>> getInquiryList(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getInquiryList(page, size));
    }
}
