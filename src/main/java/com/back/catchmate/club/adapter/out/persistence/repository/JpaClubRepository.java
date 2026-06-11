package com.back.catchmate.club.adapter.out.persistence.repository;

import com.back.catchmate.club.adapter.out.persistence.entity.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaClubRepository extends JpaRepository<ClubEntity, Long> {
}
