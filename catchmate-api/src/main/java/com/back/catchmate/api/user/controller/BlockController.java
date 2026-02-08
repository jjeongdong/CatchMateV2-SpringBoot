package com.back.catchmate.api.user.controller;

import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.user.UserBlockOrchestrator;
import com.back.catchmate.orchestration.user.dto.response.BlockActionResponse;
import com.back.catchmate.orchestration.user.dto.response.BlockedUserResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 차단 API")
@RestController
@RequestMapping("/api/users/blocks")
@RequiredArgsConstructor
public class BlockController {
    private final UserBlockOrchestrator userBlockOrchestrator;

    @PostMapping("/{targetUserId}")
    @Operation(summary = "유저 차단", description = "특정 유저를 차단합니다.")
    public ResponseEntity<BlockActionResponse> createBlock(@AuthUser Long userId, @PathVariable Long targetUserId) {
        return ResponseEntity.ok(userBlockOrchestrator.createBlock(userId, targetUserId));
    }

    @GetMapping
    @Operation(summary = "차단 목록 조회", description = "내가 차단한 유저 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<BlockedUserResponse>> getBlockList(@AuthUser Long userId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userBlockOrchestrator.getBlockList(userId, page, size));
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "유저 차단 해제", description = "차단한 유저를 해제합니다.")
    public ResponseEntity<BlockActionResponse> deleteBlock(@AuthUser Long userId, @PathVariable Long targetUserId) {
        return ResponseEntity.ok(userBlockOrchestrator.deleteBlock(userId, targetUserId));
    }
}
