package com.back.catchmate.user.presentation;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.BlockCreateResult;
import com.back.catchmate.user.application.dto.result.BlockResult;
import com.back.catchmate.user.presentation.dto.request.BlockCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다: Hibernate Validator 는 구현 메서드가 인터페이스 메서드의 파라미터 제약을 다시 선언하면(HV000151)
// 메서드 검증 시 예외를 던진다. Spring MVC 는 인터페이스 파라미터의 어노테이션도 함께 읽으므로 @Valid 도 그대로 동작한다.
@Tag(name = "[사용자] 차단 관련 API")
public interface BlockApiDocs {

    @Operation(summary = "차단 추가 API", description = "유저를 차단합니다. 이미 차단한 유저면 409 를 반환합니다.")
    ResponseEntity<BlockCreateResult> createBlock(
            @Parameter(hidden = true) Long userId, @Valid BlockCreateRequest request);

    @Operation(summary = "차단 목록 조회 API", description = "차단한 유저 목록을 최신순으로 페이징 조회합니다. size 기본 20, 최대 100.")
    ResponseEntity<OffsetPageResult<BlockResult>> getBlocks(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "차단 해제 API", description = "유저 차단을 해제합니다.")
    ResponseEntity<Void> deleteBlock(@Parameter(hidden = true) Long userId, Long blockedUserId);
}
