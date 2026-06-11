package com.back.catchmate.orchestration.club;

import com.back.catchmate.orchestration.club.dto.response.ClubResponse;
import com.back.catchmate.application.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubOrchestrator {
    private final ClubService clubService;

    public List<ClubResponse> getClubList() {
        return clubService.getClubList().stream()
                .map(ClubResponse::from)
                .toList();
    }
}
