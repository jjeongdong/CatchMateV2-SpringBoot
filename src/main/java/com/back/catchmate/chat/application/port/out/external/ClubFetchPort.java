package com.back.catchmate.chat.application.port.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatClubInfo;

import java.util.List;

public interface ClubFetchPort {
    List<ChatClubInfo> getClubs(List<Long> clubIds);
}
