package com.back.catchmate.infrastructure.persistence.club.repository;

import com.back.catchmate.domain.club.repository.ClubRepository;
import com.back.catchmate.infrastructure.persistence.club.entity.ClubEntity;
import com.back.catchmate.domain.club.model.Club;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClubRepositoryImpl implements ClubRepository {
    private final JpaClubRepository jpaClubRepository;

    @Override
    public Optional<Club> findById(Long id) {
        return jpaClubRepository.findById(id).map(ClubEntity::toModel);
    }

    @Override
    public List<Club> findAll() {
        return jpaClubRepository.findAll().stream()
                .map(ClubEntity::toModel)
                .toList();
    }
}
