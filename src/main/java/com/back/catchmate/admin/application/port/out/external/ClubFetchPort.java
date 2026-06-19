package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminClubInfo;

import java.util.List;
import java.util.Optional;

public interface ClubFetchPort {
    AdminClubInfo getClub(Long clubId);

    List<AdminClubInfo> getClubs(List<Long> clubIds);

    Optional<AdminClubInfo> findClubByName(String name);
}
