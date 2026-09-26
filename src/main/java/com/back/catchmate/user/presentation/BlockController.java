package com.back.catchmate.user.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.BlockCommandService;
import com.back.catchmate.user.application.BlockQueryService;
import com.back.catchmate.user.application.dto.result.BlockCreateResult;
import com.back.catchmate.user.application.dto.result.BlockResult;
import com.back.catchmate.user.presentation.dto.request.BlockCreateRequest;
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

// @Validated 를 붙이지 않는다: 붙이면 AOP 검증이 ConstraintViolationException(500)을 던진다.
// 파라미터 제약(BlockApiDocs 에 선언)은 Spring 6.1 기본 메서드 검증이 HandlerMethodValidationException(400)으로 처리한다.
@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController implements BlockApiDocs {
    private final BlockCommandService blockCommandService;
    private final BlockQueryService blockQueryService;

    @Override
    @PostMapping
    public ResponseEntity<BlockCreateResult> createBlock(
            @AuthUser Long userId, @RequestBody BlockCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blockCommandService.createBlock(userId, request.toCommand()));
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<BlockResult>> getBlocks(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(blockQueryService.getBlocks(userId, page, size));
    }

    @Override
    @DeleteMapping("/{blockedUserId}")
    public ResponseEntity<Void> deleteBlock(@AuthUser Long userId, @PathVariable Long blockedUserId) {
        blockCommandService.deleteBlock(userId, blockedUserId);
        return ResponseEntity.noContent().build();
    }
}
