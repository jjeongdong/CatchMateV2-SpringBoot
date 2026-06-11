package com.back.catchmate.club.application.port.out;

import com.back.catchmate.club.domain.model.Club;

import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    Optional<Club> findById(Long id);
    List<Club> findAll();
}
