package com.back.catchmate.club.service;

import com.back.catchmate.club.dto.response.ClubResponse;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.entity.Club;
import com.back.catchmate.club.repository.ClubRepository;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {
    private final ClubRepository clubRepository;

    public List<ClubResponse> getClubList() {
        return clubRepository.findAll().stream()
                .map(ClubResponse::from)
                .toList();
    }

    public ClubSummary getClubSummary(Long clubId) {
        return toSummary(getClubOrThrow(clubId));
    }

    public List<ClubSummary> getClubSummaries(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return List.of();
        }
        return clubRepository.findAllById(clubIds).stream()
                .map(this::toSummary)
                .toList();
    }

    public Optional<ClubSummary> findClubSummaryByName(String name) {
        return clubRepository.findByName(name).map(this::toSummary);
    }

    private Club getClubOrThrow(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BaseException(ErrorCode.CLUB_NOT_FOUND));
    }

    private ClubSummary toSummary(Club club) {
        return new ClubSummary(club.getId(), club.getName(), club.getHomeStadium(), club.getRegion());
    }
}
