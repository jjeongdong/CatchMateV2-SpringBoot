package com.back.catchmate.oauth.application.port.out;

import com.back.catchmate.club.domain.model.Club;

public interface ClubFetchPort {
    Club getClub(Long clubId);
}
