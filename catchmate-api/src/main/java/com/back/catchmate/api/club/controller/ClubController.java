package com.back.catchmate.api.club.controller;

import com.back.catchmate.application.club.ClubUseCase;
import com.back.catchmate.application.club.dto.ClubResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "[사용자] 구단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
public class ClubController {
    private final ClubUseCase clubUseCase;

    @GetMapping("/list")
    @Operation(summary = "구단 정보 리스트 조회 API", description = "구단 정보를 리스트로 조회하는 API 입니다.")
    public List<ClubResponse> getClubResponseList() {
        return clubUseCase.getClubResponseList();
    }
}
