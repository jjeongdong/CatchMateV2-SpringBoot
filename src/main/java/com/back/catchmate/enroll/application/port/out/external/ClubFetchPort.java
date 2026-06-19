package com.back.catchmate.enroll.application.port.out.external;

import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;

import java.util.List;

public interface ClubFetchPort {
    EnrollClubInfo getClub(Long clubId);

    List<EnrollClubInfo> getClubs(List<Long> clubIds);
}
