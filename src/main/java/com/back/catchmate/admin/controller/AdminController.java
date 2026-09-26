package com.back.catchmate.admin.controller;

import com.back.catchmate.admin.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.dto.response.AdminUserResponse;
import com.back.catchmate.admin.service.AdminService;
import com.back.catchmate.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
