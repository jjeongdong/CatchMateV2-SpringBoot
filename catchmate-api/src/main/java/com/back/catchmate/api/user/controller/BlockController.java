package com.back.catchmate.api.user.controller;

import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.user.UserBlockUseCase;
import com.back.catchmate.application.user.dto.response.BlockActionResponse;
import com.back.catchmate.application.user.dto.response.BlockedUserResponse;
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
    private final UserBlockUseCase userBlockUseCase;

    @PostMapping("/{targetUserId}")
    @Operation(summary = "유저 차단", description = "특정 유저를 차단합니다.")
    public ResponseEntity<BlockActionResponse> blockUser(@AuthUser Long userId, @PathVariable Long targetUserId) {
        return ResponseEntity.ok(userBlockUseCase.blockUser(userId, targetUserId));
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "유저 차단 해제", description = "차단한 유저를 해제합니다.")
    public ResponseEntity<BlockActionResponse> unblockUser(@AuthUser Long userId, @PathVariable Long targetUserId) {
        userBlockUseCase.unblockUser(userId, targetUserId);
        return ResponseEntity.ok(userBlockUseCase.unblockUser(userId, targetUserId));
    }

    @GetMapping
    @Operation(summary = "차단 목록 조회", description = "내가 차단한 유저 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<BlockedUserResponse>> getBlockedList(@AuthUser Long userId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userBlockUseCase.getBlockedList(userId, page, size));
    }
}
