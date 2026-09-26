package com.back.catchmate.admin.application;

import com.back.catchmate.admin.application.dto.result.AdminDashboardResult;
import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.inquiry.application.InquiryQueryApi;
import com.back.catchmate.report.application.ReportQueryApi;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// admin 은 자기 데이터가 없고 여러 BC 의 수를 모아 보여주기만 한다.
@Service
@RequiredArgsConstructor
public class AdminQueryService {
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;
    private final ReportQueryApi reportQueryApi;
    private final InquiryQueryApi inquiryQueryApi;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public AdminDashboardResult getDashboard() {
        return new AdminDashboardResult(
                userQueryApi.count(),
                new AdminDashboardResult.GenderRatio(userQueryApi.countByGender('M'), userQueryApi.countByGender('F')),
                boardQueryApi.count(),
                countUsersByClubName(),
                userQueryApi.countByWatchStyles(),
                reportQueryApi.count(),
                reportQueryApi.countPending(),
                inquiryQueryApi.count(),
                inquiryQueryApi.countWaiting());
    }

    // 구단 정보가 없는 구단 ID 의 집계는 이름을 붙일 수 없어 뺀다 (옛 동작).
    private Map<String, Long> countUsersByClubName() {
        Map<Long, Long> countByClubId = userQueryApi.countByClubIds();
        if (countByClubId.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubInfo> clubById = clubQueryApi.getInfos(List.copyOf(countByClubId.keySet()));
        return countByClubId.entrySet().stream()
                .filter(entry -> clubById.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> clubById.get(entry.getKey()).name(), Map.Entry::getValue));
    }
}
