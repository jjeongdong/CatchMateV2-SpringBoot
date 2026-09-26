package com.back.catchmate.club.application;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.club.domain.ClubRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubQueryApi {
    private final ClubRepository clubRepository;

    /**
     * 구단 하나를 조회한다. 구단이 없으면 {@code ClubNotFoundException}(404 CLUB_NOT_FOUND)을 던지므로 존재 검증에도 쓸 수 있다.
     *
     * @param clubId 구단 ID
     * @return 구단 정보
     */
    @Transactional(readOnly = true)
    public ClubInfo getInfo(Long clubId) {
        return ClubInfo.from(clubRepository.getById(clubId));
    }

    /**
     * 여러 구단을 한 번의 쿼리로 조회한다. 없는 ID 는 결과 맵에서 빠진다.
     *
     * @param clubIds 구단 ID 목록
     * @return 구단 ID 를 키로 한 구단 정보 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, ClubInfo> getInfos(Collection<Long> clubIds) {
        return clubRepository.findAllByIds(clubIds).stream()
                .map(ClubInfo::from)
                .collect(Collectors.toMap(ClubInfo::clubId, Function.identity()));
    }

    /**
     * 구단명으로 구단을 조회한다.
     *
     * @param name 구단명 (예: "LG 트윈스")
     * @return 구단 정보, 없으면 빈 값
     */
    @Transactional(readOnly = true)
    public Optional<ClubInfo> findInfoByName(String name) {
        return clubRepository.findByName(name).map(ClubInfo::from);
    }
}
