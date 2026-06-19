package com.back.catchmate.user.adapter.in.web.controller;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.port.in.BlockClientCommandUseCase;
import com.back.catchmate.user.application.port.in.BlockClientQueryUseCase;
import com.back.catchmate.user.application.dto.response.BlockActionResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;
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

@Tag(name = "[사용자] 차단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class BlockController {
    private final BlockClientCommandUseCase blockClientCommandUseCase;
    private final BlockClientQueryUseCase blockClientQueryUseCase;

    @PostMapping("/{blockedId}")
    @Operation(summary = "차단 추가 API", description = "유저 차단을 추가하는 API 입니다.")
    public ResponseEntity<BlockActionResponse> createBlock(@AuthUser Long userId, @PathVariable Long blockedId) {
        return ResponseEntity.ok(blockClientCommandUseCase.createBlock(userId, blockedId));
    }

    @GetMapping
    @Operation(summary = "차단 목록 조회 API", description = "차단한 유저 목록을 페이징 조회하는 API 입니다.")
    public ResponseEntity<PagedResponse<BlockedUserResponse>> getBlockList(@AuthUser Long userId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(blockClientQueryUseCase.getBlockList(userId, page, size));
    }

    @DeleteMapping("/{blockedId}")
    @Operation(summary = "차단 해제 API", description = "유저 차단을 해제하는 API 입니다.")
    public ResponseEntity<BlockActionResponse> deleteBlock(@AuthUser Long userId, @PathVariable Long blockedId) {
        return ResponseEntity.ok(blockClientCommandUseCase.deleteBlock(userId, blockedId));
    }
}
