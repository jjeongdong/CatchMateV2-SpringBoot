package com.back.catchmate.club.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    Club getById(Long clubId);

    Optional<Club> findByName(String name);

    List<Club> findAllByIds(Collection<Long> clubIds);

    List<Club> findAll();
}
