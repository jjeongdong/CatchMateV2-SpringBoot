package com.back.catchmate.api.enroll.controller;

import com.back.catchmate.api.enroll.dto.request.EnrollCreateRequest;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.common.permission.PermissionId;
import com.back.catchmate.global.annotation.AuthUser;
import com.back.catchmate.global.aop.permission.CheckEnrollApplicantPermission;
import com.back.catchmate.global.aop.permission.CheckEnrollHostPermission;
import com.back.catchmate.orchestration.enroll.EnrollOrchestrator;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollApplicantResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCancelResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCountResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCreateResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollDetailResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollReceiveResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollRejectResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 직관 신청 관련 API")
@RestController
@RequiredArgsConstructor
public class EnrollController {
    private final EnrollOrchestrator enrollOrchestrator;

    @PostMapping("/api/boards/{boardId}/enrolls")
    @Operation(summary = "직관 신청 등록", description = "게시글에 대해 직관 신청을 합니다.")
    public ResponseEntity<EnrollCreateResponse> createEnroll(@AuthUser Long userId,
                                                             @PathVariable Long boardId,
                                                             @Valid @RequestBody EnrollCreateRequest request) {
        return ResponseEntity.ok(enrollOrchestrator.createEnroll(request.toCommand(userId, boardId)));
    }

    @GetMapping("/api/enrolls/{enrollId}")
    @Operation(summary = "직관 신청 단일 상세 조회", description = "특정 신청 내역(enrollId)의 상세 정보를 조회합니다.")
    public ResponseEntity<EnrollDetailResponse> getEnroll(@AuthUser Long userId,
                                                          @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollOrchestrator.getEnroll(userId, enrollId));
    }

    @GetMapping("/api/enrolls/request")
    @Operation(summary = "내가 보낸 직관 신청 목록 조회", description = "내가 신청한 직관 신청 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<EnrollRequestResponse>> getEnrollRequestList(@AuthUser Long userId,
                                                                                     @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(enrollOrchestrator.getEnrollRequestList(userId, page, size));
    }

    @GetMapping("/api/enrolls/receive")
    @Operation(summary = "내가 받은 직관 신청 목록 조회 (단일 게시글 상세)", description = "특정 게시글(boardId)에 들어온 신청자 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<EnrollApplicantResponse>> getEnrollReceiveListByBoardId(@AuthUser Long userId,
                                                                                                @RequestParam Long boardId,
                                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(enrollOrchestrator.getEnrollReceiveListByBoardId(userId, boardId, page, size));
    }

    @GetMapping("/api/enrolls/receive/all")
    @Operation(summary = "내가 받은 직관 신청 목록 조회 (전체 게시글)", description = "게시글 단위로 페이징하며, 각 게시글에는 신청자 목록이 포함됩니다.")
    public ResponseEntity<PagedResponse<EnrollReceiveResponse>> getEnrollReceiveList(@AuthUser Long userId,
                                                                                     @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(enrollOrchestrator.getEnrollReceiveList(userId, page, size));
    }

    @GetMapping("/api/enrolls/count")
    @Operation(summary = "내 게시글 신청 갯수 조회 API", description = "내가 작성한 게시글에 들어온 '대기 중'인 신청의 총 갯수를 반환합니다.")
    public ResponseEntity<EnrollCountResponse> getEnrollPendingCount(@AuthUser Long userId) {
        return ResponseEntity.ok(enrollOrchestrator.getEnrollPendingCount(userId));
    }

    @CheckEnrollHostPermission
    @PatchMapping("/api/enrolls/{enrollId}/accept")
    @Operation(summary = "직관 신청 수락 API", description = "들어온 직관 신청을 수락합니다.")
    public ResponseEntity<EnrollAcceptResponse> updateEnrollAccept(@AuthUser Long userId,
                                                                   @PermissionId @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollOrchestrator.updateEnrollAccept(userId, enrollId));
    }

    @CheckEnrollHostPermission
    @PatchMapping("/api/enrolls/{enrollId}/reject")
    @Operation(summary = "직관 신청 거절 API", description = "들어온 직관 신청을 거절합니다.")
    public ResponseEntity<EnrollRejectResponse> updateEnrollReject(@AuthUser Long userId,
                                                                   @PermissionId @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollOrchestrator.updateEnrollReject(userId, enrollId));
    }

    @CheckEnrollApplicantPermission
    @DeleteMapping("/api/enrolls/{enrollId}")
    @Operation(summary = "직관 신청 취소 API", description = "직관 신청을 취소하는 API 입니다.")
    public ResponseEntity<EnrollCancelResponse> deleteEnroll(@AuthUser Long userId,
                                                             @PermissionId @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollOrchestrator.deleteEnroll(userId, enrollId));
    }
}
