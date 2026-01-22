package com.back.catchmate.domain.club.service;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.repository.ClubRepository;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final ClubRepository clubRepository;

    public Club getClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(ErrorCode.CLUB_NOT_FOUND));
    }

    public List<Club> getClubList() {
        return clubRepository.findAll();
    }
}
