package com.back.catchmate.enroll.presentation;

import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.application.dto.result.EnrollApplicantResult;
import com.back.catchmate.enroll.application.dto.result.EnrollCreateResult;
import com.back.catchmate.enroll.application.dto.result.EnrollDetailResult;
import com.back.catchmate.enroll.application.dto.result.EnrollPendingCountResult;
import com.back.catchmate.enroll.application.dto.result.EnrollReceivedResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRejectResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRequestResult;
import com.back.catchmate.enroll.presentation.dto.request.EnrollCreateRequest;
import com.back.catchmate.global.response.OffsetPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[사용자] 직관 신청 관련 API")
public interface EnrollApiDocs {

    @Operation(summary = "직관 신청 등록", description = "게시글에 대해 직관 신청을 합니다. (201)")
    ResponseEntity<EnrollCreateResult> createEnroll(
            @Parameter(hidden = true) Long userId, Long boardId, @Valid EnrollCreateRequest request);

    @Operation(summary = "직관 신청 단일 상세 조회", description = "신청자 또는 게시글 작성자만 조회할 수 있습니다. 읽음 처리는 별도 API 로 요청하세요.")
    ResponseEntity<EnrollDetailResult> getEnroll(@Parameter(hidden = true) Long userId, Long enrollId);

    @Operation(summary = "직관 신청 읽음 처리", description = "게시글 작성자가 새 신청을 확인했음을 표시합니다. (204)")
    ResponseEntity<Void> markEnrollAsRead(@Parameter(hidden = true) Long userId, Long enrollId);

    @Operation(summary = "내가 보낸 직관 신청 목록 조회", description = "내가 신청한 직관 신청 목록을 최신순으로 조회합니다.")
    ResponseEntity<OffsetPageResult<EnrollRequestResult>> getMyEnrolls(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "게시글의 신청자 목록 조회", description = "내 게시글에 들어온 대기 중 신청자를 최신순으로 조회합니다.")
    ResponseEntity<OffsetPageResult<EnrollApplicantResult>> getBoardEnrolls(
            @Parameter(hidden = true) Long userId, Long boardId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "내가 받은 직관 신청 목록 조회", description = "게시글 단위로 페이징하며, 각 게시글에는 대기 중 신청자 목록이 포함됩니다.")
    ResponseEntity<OffsetPageResult<EnrollReceivedResult>> getReceivedEnrolls(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "받은 대기 중 신청 수 조회", description = "내 게시글에 들어온 대기 중 신청의 총 개수를 반환합니다.")
    ResponseEntity<EnrollPendingCountResult> getPendingEnrollCount(@Parameter(hidden = true) Long userId);

    @Operation(summary = "직관 신청 수락", description = "들어온 직관 신청을 수락합니다. 정원이 찼으면 409 BOARD_FULL.")
    ResponseEntity<EnrollAcceptResult> acceptEnroll(@Parameter(hidden = true) Long userId, Long enrollId);

    @Operation(summary = "직관 신청 거절", description = "들어온 직관 신청을 거절합니다.")
    ResponseEntity<EnrollRejectResult> rejectEnroll(@Parameter(hidden = true) Long userId, Long enrollId);

    @Operation(summary = "직관 신청 취소", description = "내 직관 신청을 취소합니다. (204)")
    ResponseEntity<Void> deleteEnroll(@Parameter(hidden = true) Long userId, Long enrollId);
}
