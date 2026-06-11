package com.back.catchmate.domain.club.repository;

import com.back.catchmate.domain.club.model.Club;

import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    Optional<Club> findById(Long id);
    List<Club> findAll();
}
