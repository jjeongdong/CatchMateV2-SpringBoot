package com.back.catchmate.club.adapter.out.persistence.repository;

import com.back.catchmate.club.application.port.out.persistence.ClubRepository;
import com.back.catchmate.club.adapter.out.persistence.entity.ClubEntity;
import com.back.catchmate.club.domain.model.Club;
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
        return jpaClubRepository.findById(id)
                .map(ClubEntity::toDomain);
    }

    @Override
    public Optional<Club> findByName(String name) {
        return jpaClubRepository.findByName(name)
                .map(ClubEntity::toDomain);
    }

    @Override
    public List<Club> findAll() {
        return jpaClubRepository.findAll().stream()
                .map(ClubEntity::toDomain)
                .toList();
    }

    @Override
    public List<Club> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaClubRepository.findAllById(ids).stream()
                .map(ClubEntity::toDomain)
                .toList();
    }
}
