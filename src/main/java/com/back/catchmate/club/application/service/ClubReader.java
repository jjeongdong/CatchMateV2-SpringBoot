package com.back.catchmate.club.application.service;

import com.back.catchmate.club.application.port.out.persistence.ClubRepository;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClubReader {
    private final ClubRepository clubRepository;

    public Club getClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(ErrorCode.CLUB_NOT_FOUND));
    }

    public List<Club> getClubs(List<Long> clubIds) {
        return clubRepository.findAllByIds(clubIds);
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    public Optional<Club> findByName(String name) {
        return clubRepository.findByName(name);
    }
}
