package com.back.catchmate.club.application;

import com.back.catchmate.club.application.dto.result.ClubResult;
import com.back.catchmate.club.domain.ClubRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubQueryService {
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public List<ClubResult> getClubs() {
        return clubRepository.findAll().stream().map(ClubResult::from).toList();
    }
}
