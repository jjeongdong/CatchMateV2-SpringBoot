package com.back.catchmate.club.application.service;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.application.port.in.ClubClientQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubClientQueryService implements ClubClientQueryUseCase {
    private final ClubReader clubReader;

    @Override
    public List<ClubResponse> getClubList() {
        return clubReader.getAllClubs().stream()
                .map(ClubResponse::from)
                .toList();
    }
}
