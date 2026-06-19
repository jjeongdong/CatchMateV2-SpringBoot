package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationClubInfo;

import java.util.List;

public interface ClubFetchPort {
    NotificationClubInfo getClub(Long clubId);

    List<NotificationClubInfo> getClubs(List<Long> clubIds);
}
