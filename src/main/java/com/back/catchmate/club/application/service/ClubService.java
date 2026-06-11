package com.back.catchmate.club.application.service;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.application.port.in.ClubUseCase;
import com.back.catchmate.club.application.port.out.ClubRepository;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService implements ClubUseCase {


    public List<ClubResponse> getClubList() {
        return getAllClubs().stream()
                .map(ClubResponse::from)
                .toList();
    }


    private final ClubRepository clubRepository;

    public Club getClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(ErrorCode.CLUB_NOT_FOUND));
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }
}
