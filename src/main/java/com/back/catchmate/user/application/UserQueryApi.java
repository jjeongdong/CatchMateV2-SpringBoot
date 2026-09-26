package com.back.catchmate.user.application;

import com.back.catchmate.user.application.dto.api.UserInfo;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryApi {
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    /**
     * 유저 하나를 조회한다. 없거나 탈퇴했으면 {@code UserNotFoundException}(404 USER_NOT_FOUND)을 던지므로 존재 검증에도 쓸 수 있다.
     *
     * @param userId 유저 ID
     * @return 유저 정보
     */
    @Transactional(readOnly = true)
    public UserInfo getInfo(Long userId) {
        return UserInfo.from(userRepository.getById(userId));
    }

    /**
     * 여러 유저를 한 번의 쿼리로 조회한다. 없거나 탈퇴한 ID 는 결과 맵에서 빠진다.
     *
     * @param userIds 유저 ID 목록
     * @return 유저 ID 를 키로 한 유저 정보 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, UserInfo> getInfos(Collection<Long> userIds) {
        return userRepository.findAllByIds(userIds).stream()
                .map(UserInfo::from)
                .collect(Collectors.toMap(UserInfo::userId, Function.identity()));
    }

    /**
     * OAuth 제공자 식별자로 가입된 유저를 찾는다.
     *
     * @param providerIdWithProvider 제공자 접두사가 붙은 식별자 (예: "KAKAO_12345")
     * @return 유저 정보, 가입 전이면 빈 값
     */
    @Transactional(readOnly = true)
    public Optional<UserInfo> findInfoByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider).map(UserInfo::from);
    }

    /**
     * 이벤트 알림을 켠 유저 전체를 조회한다 (공지 알림 대상).
     *
     * @return 유저 정보 목록
     */
    @Transactional(readOnly = true)
    public List<UserInfo> getEventAlarmEnabledInfos() {
        return userRepository.findAllEventAlarmEnabled().stream()
                .map(UserInfo::from)
                .toList();
    }

    /**
     * 구단마다 유저 수를 센다.
     *
     * @return 구단 ID 를 키로 한 유저 수
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countByClubIds() {
        return userRepository.countGroupedByClubId();
    }

    /**
     * 응원 스타일마다 유저 수를 센다. 스타일이 없는 유저는 빠진다.
     *
     * @return 응원 스타일을 키로 한 유저 수
     */
    @Transactional(readOnly = true)
    public Map<String, Long> countByWatchStyles() {
        return userRepository.countGroupedByWatchStyle();
    }

    /**
     * 전체 유저 수를 센다.
     *
     * @return 유저 수
     */
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    /**
     * 성별 유저 수를 센다.
     *
     * @param gender 'M' 또는 'F'
     * @return 유저 수
     */
    @Transactional(readOnly = true)
    public long countByGender(Character gender) {
        return userRepository.countByGender(gender);
    }

    /**
     * blocker 가 blocked 를 차단했는지 확인한다.
     *
     * @param blockerId 차단한 유저 ID
     * @param blockedId 차단 여부를 볼 대상 유저 ID
     * @return 차단했으면 true
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * 유저가 차단한 유저 ID 를 모두 조회한다.
     *
     * @param blockerId 차단한 유저 ID
     * @return 차단된 유저 ID 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findBlockedIdsByBlockerId(blockerId);
    }
}
