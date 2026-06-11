package com.back.catchmate.infrastructure.persistence.club.repository;

import com.back.catchmate.infrastructure.persistence.club.entity.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaClubRepository extends JpaRepository<ClubEntity, Long> {
}
