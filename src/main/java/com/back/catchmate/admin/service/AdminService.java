package com.back.catchmate.admin.service;

import com.back.catchmate.admin.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.dto.response.AdminUserResponse;
import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.inquiry.application.InquiryQueryApi;
import com.back.catchmate.report.application.ReportQueryApi;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {
    private final ClubQueryApi clubQueryApi;
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;
    private final ReportQueryApi reportQueryApi;
    private final InquiryQueryApi inquiryQueryApi;

    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.of(
                userQueryApi.count(),
                AdminDashboardResponse.GenderRatio.of(userQueryApi.countByGender('M'), userQueryApi.countByGender('F')),
                boardQueryApi.count(),
                resolveUserCountByClubName(),
                userQueryApi.countByWatchStyles(),
                reportQueryApi.count(),
                reportQueryApi.countPending(),
                inquiryQueryApi.count(),
                inquiryQueryApi.countWaiting());
    }

    private Map<String, Long> resolveUserCountByClubName() {
        Map<Long, Long> countByClubId = userQueryApi.countByClubIds();
        if (countByClubId.isEmpty()) return Map.of();
        Map<Long, ClubInfo> clubById = clubQueryApi.getInfos(List.copyOf(countByClubId.keySet()));
        return countByClubId.entrySet().stream()
                .filter(e -> clubById.get(e.getKey()) != null)
                .collect(Collectors.toMap(e -> clubById.get(e.getKey()).name(), Map.Entry::getValue));
    }

    public AdminUserDetailResponse getUser(Long userId) {
        UserInfo user = userQueryApi.getInfo(userId);
        ClubInfo club = user.clubId() != null ? clubQueryApi.getInfo(user.clubId()) : null;
        return AdminUserDetailResponse.from(user, club != null ? club.name() : null);
    }

    public PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Long clubId = null;
        if (clubName != null && !clubName.isBlank()) {
            Optional<ClubInfo> club = clubQueryApi.findInfoByName(clubName);
            if (club.isEmpty()) {
                return new PagedResponse<>(Page.empty(pageable), List.of());
            }
            clubId = club.get().clubId();
        }

        Page<UserInfo> userPage = new PageImpl<>(
                userQueryApi.getInfosByClubId(clubId, page, size), pageable, userQueryApi.countByClubId(clubId));

        Map<Long, ClubInfo> clubById = resolveUserClubs(userPage.getContent());

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(u -> AdminUserResponse.from(
                        u,
                        u.clubId() != null && clubById.get(u.clubId()) != null
                                ? clubById.get(u.clubId()).name()
                                : null))
                .toList();

        return new PagedResponse<>(userPage, responses);
    }

    private Map<Long, ClubInfo> resolveUserClubs(Collection<UserInfo> users) {
        List<Long> clubIds = users.stream()
                .map(UserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) return Map.of();
        return clubQueryApi.getInfos(clubIds);
    }
}
