package com.back.catchmate.club.application.service;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.application.service.ClubService;
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
