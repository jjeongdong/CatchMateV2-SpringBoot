package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkClubInfo;
import com.back.catchmate.bookmark.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookmarkClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public List<BookmarkClubInfo> getClubs(List<Long> clubIds) {
        List<ClubSummary> clubs = clubService.getClubSummaries(clubIds);

        return clubs.stream()
                .map(club -> new BookmarkClubInfo(
                        club.clubId(),
                        club.name()
                ))
                .toList();
    }
}
