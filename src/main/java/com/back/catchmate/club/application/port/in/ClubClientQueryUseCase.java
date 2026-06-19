package com.back.catchmate.club.application.port.in;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import java.util.List;

public interface ClubClientQueryUseCase {
    List<ClubResponse> getClubList();
}
