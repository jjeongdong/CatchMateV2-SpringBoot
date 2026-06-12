package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.club.domain.model.Club;

import java.util.List;

public interface ClubFetchPort {
    Club getClub(Long clubId);
    List<Club> getClubs(List<Long> clubIds);
}
