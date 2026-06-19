package com.back.catchmate.user.application.port.out.external;

import com.back.catchmate.user.application.port.out.dto.UserClubInfo;

public interface ClubFetchPort {
    UserClubInfo getClub(Long clubId);
}
