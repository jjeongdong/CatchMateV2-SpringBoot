package com.back.catchmate.club.application.port.out.persistence;

import com.back.catchmate.club.domain.model.Club;

import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    Optional<Club> findById(Long id);

    Optional<Club> findByName(String name);

    List<Club> findAll();

    List<Club> findAllByIds(List<Long> ids);
}
