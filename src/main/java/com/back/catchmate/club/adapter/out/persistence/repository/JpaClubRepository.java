package com.back.catchmate.club.adapter.out.persistence.repository;

import com.back.catchmate.club.adapter.out.persistence.entity.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaClubRepository extends JpaRepository<ClubEntity, Long> {
    Optional<ClubEntity> findByName(String name);
}
