package com.back.catchmate.club.infrastructure;

import com.back.catchmate.club.domain.Club;
import com.back.catchmate.club.domain.ClubRepository;
import com.back.catchmate.club.domain.exception.ClubNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClubRepositoryImpl implements ClubRepository {
    private final ClubJpaRepository clubJpaRepository;

    @Override
    public Club getById(Long clubId) {
        return clubJpaRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    }

    @Override
    public Optional<Club> findByName(String name) {
        return clubJpaRepository.findByName(name);
    }

    @Override
    public List<Club> findAllByIds(Collection<Long> clubIds) {
        return clubJpaRepository.findAllById(clubIds);
    }

    @Override
    public List<Club> findAll() {
        return clubJpaRepository.findAll();
    }
}
