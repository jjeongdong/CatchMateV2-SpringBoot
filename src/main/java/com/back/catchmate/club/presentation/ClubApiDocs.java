package com.back.catchmate.club.presentation;

import com.back.catchmate.club.application.dto.result.ClubResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 구단 관련 API")
public interface ClubApiDocs {

    @Operation(summary = "구단 목록 조회 API", description = "전체 구단 목록을 조회하는 API 입니다.")
    ResponseEntity<List<ClubResult>> getClubs();
}
