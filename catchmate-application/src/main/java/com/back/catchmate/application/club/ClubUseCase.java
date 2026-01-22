package com.back.catchmate.application.club;

import com.back.catchmate.application.club.dto.ClubResponse;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClubUseCase {
    private final ClubService clubService;

    @Transactional(readOnly = true)
    public List<ClubResponse> getClubResponseList() {
        List<Club> clubs = clubService.getClubList();
        return clubs.stream()
                .map(ClubResponse::from)
                .toList();
    }
}
