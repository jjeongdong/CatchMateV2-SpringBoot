package com.back.catchmate.enroll.presentation;

import com.back.catchmate.enroll.application.EnrollCommandService;
import com.back.catchmate.enroll.application.EnrollQueryService;
import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.application.dto.result.EnrollApplicantResult;
import com.back.catchmate.enroll.application.dto.result.EnrollCreateResult;
import com.back.catchmate.enroll.application.dto.result.EnrollDetailResult;
import com.back.catchmate.enroll.application.dto.result.EnrollPendingCountResult;
import com.back.catchmate.enroll.application.dto.result.EnrollReceivedResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRejectResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRequestResult;
import com.back.catchmate.enroll.presentation.dto.request.EnrollCreateRequest;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.OffsetPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 신청은 게시글 하위(/boards/{boardId}/enrolls)와 신청 자체(/enrolls/**) 두 경로에 걸쳐 있어 클래스 매핑은 /api 까지만 둔다.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollController implements EnrollApiDocs {
    private final EnrollCommandService enrollCommandService;
    private final EnrollQueryService enrollQueryService;

    @Override
    @PostMapping("/boards/{boardId}/enrolls")
    public ResponseEntity<EnrollCreateResult> createEnroll(
            @AuthUser Long userId, @PathVariable Long boardId, @RequestBody EnrollCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollCommandService.createEnroll(userId, boardId, request.toCommand()));
    }

    @Override
    @GetMapping("/enrolls/{enrollId}")
    public ResponseEntity<EnrollDetailResult> getEnroll(@AuthUser Long userId, @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollQueryService.getEnroll(userId, enrollId));
    }

    @Override
    @PostMapping("/enrolls/{enrollId}/read")
    public ResponseEntity<Void> markEnrollAsRead(@AuthUser Long userId, @PathVariable Long enrollId) {
        enrollCommandService.markEnrollAsRead(userId, enrollId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/enrolls/me")
    public ResponseEntity<OffsetPageResult<EnrollRequestResult>> getMyEnrolls(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(enrollQueryService.getMyEnrolls(userId, page, size));
    }

    @Override
    @GetMapping("/boards/{boardId}/enrolls")
    public ResponseEntity<OffsetPageResult<EnrollApplicantResult>> getBoardEnrolls(
            @AuthUser Long userId,
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(enrollQueryService.getBoardEnrolls(userId, boardId, page, size));
    }

    @Override
    @GetMapping("/enrolls/received")
    public ResponseEntity<OffsetPageResult<EnrollReceivedResult>> getReceivedEnrolls(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(enrollQueryService.getReceivedEnrolls(userId, page, size));
    }

    @Override
    @GetMapping("/enrolls/received/pending-count")
    public ResponseEntity<EnrollPendingCountResult> getPendingEnrollCount(@AuthUser Long userId) {
        return ResponseEntity.ok(enrollQueryService.getPendingEnrollCount(userId));
    }

    @Override
    @PostMapping("/enrolls/{enrollId}/accept")
    public ResponseEntity<EnrollAcceptResult> acceptEnroll(@AuthUser Long userId, @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollCommandService.acceptEnroll(userId, enrollId));
    }

    @Override
    @PostMapping("/enrolls/{enrollId}/reject")
    public ResponseEntity<EnrollRejectResult> rejectEnroll(@AuthUser Long userId, @PathVariable Long enrollId) {
        return ResponseEntity.ok(enrollCommandService.rejectEnroll(userId, enrollId));
    }

    @Override
    @DeleteMapping("/enrolls/{enrollId}")
    public ResponseEntity<Void> deleteEnroll(@AuthUser Long userId, @PathVariable Long enrollId) {
        enrollCommandService.deleteEnroll(userId, enrollId);
        return ResponseEntity.noContent().build();
    }
}
