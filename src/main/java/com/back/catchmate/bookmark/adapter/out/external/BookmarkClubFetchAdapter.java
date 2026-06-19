package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkClubInfo;
import com.back.catchmate.bookmark.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookmarkClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public List<BookmarkClubInfo> getClubs(List<Long> clubIds) {
        List<ClubInternalResponse> clubs = clubInternalQueryUseCase.getClubs(clubIds);

        return clubs.stream()
                .map(club -> new BookmarkClubInfo(
                        club.clubId(),
                        club.name()
                ))
                .toList();
    }
}
