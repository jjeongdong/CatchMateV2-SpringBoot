package com.back.catchmate.club.infrastructure;

import com.back.catchmate.club.domain.Club;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubJpaRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByName(String name);
}
