package com.back.catchmate.user.application;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.AdminUserDetailResult;
import com.back.catchmate.user.application.dto.result.AdminUserResult;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {
    private final UserRepository userRepository;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public UserResult getMyProfile(Long userId) {
        return getUser(userId);
    }

    @Transactional(readOnly = true)
    public UserResult getUser(Long userId) {
        User user = userRepository.getById(userId);
        return UserResult.of(user, clubQueryApi.getInfo(user.getClubId()));
    }

    @Transactional(readOnly = true)
    public NicknameAvailabilityResult getNicknameAvailability(String nickName) {
        return new NicknameAvailabilityResult(nickName, !userRepository.existsByNickName(nickName));
    }

    @Transactional(readOnly = true)
    public UserAlarmResult getMyAlarms(Long userId) {
        return UserAlarmResult.from(userRepository.getById(userId));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResult getAdminUser(Long userId) {
        User user = userRepository.getById(userId);
        String clubName = user.getClubId() != null
                ? clubQueryApi.getInfo(user.getClubId()).name()
                : null;
        return AdminUserDetailResult.of(user, clubName);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<AdminUserResult> getAdminUsers(String clubName, int page, int size) {
        Long clubId = null;
        if (clubName != null && !clubName.isBlank()) {
            Optional<ClubInfo> club = clubQueryApi.findInfoByName(clubName);
            // 없는 구단으로 거르면 전체가 아니라 빈 목록이어야 한다 (옛 동작).
            if (club.isEmpty()) {
                return OffsetPageResult.of(List.of(), page, size, 0);
            }
            clubId = club.orElseThrow().clubId();
        }
        List<User> users = userRepository.findAllByClubId(clubId, (long) page * size, size);
        long totalElements = userRepository.countByClubId(clubId);
        List<Long> clubIds = users.stream()
                .map(User::getClubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
        List<AdminUserResult> content = users.stream()
                .map(user -> {
                    ClubInfo club = user.getClubId() != null ? clubById.get(user.getClubId()) : null;
                    return AdminUserResult.of(user, club != null ? club.name() : null);
                })
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }
}
