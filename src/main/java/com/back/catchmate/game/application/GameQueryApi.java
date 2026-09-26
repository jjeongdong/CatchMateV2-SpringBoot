package com.back.catchmate.game.application;

import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.game.domain.GameRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameQueryApi {
    private final GameRepository gameRepository;

    /**
     * 경기 하나를 조회한다. 경기가 없으면 {@code GameNotFoundException}(404 GAME_NOT_FOUND)을 던지므로 존재 검증에도 쓸 수 있다.
     *
     * @param gameId 경기 ID
     * @return 경기 정보
     */
    @Transactional(readOnly = true)
    public GameInfo getInfo(Long gameId) {
        return GameInfo.from(gameRepository.getById(gameId));
    }

    /**
     * 여러 경기를 한 번의 쿼리로 조회한다. 없는 ID 는 결과 맵에서 빠진다.
     *
     * @param gameIds 경기 ID 목록
     * @return 경기 ID 를 키로 한 경기 정보 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, GameInfo> getInfos(Collection<Long> gameIds) {
        return gameRepository.findAllByIds(gameIds).stream()
                .map(GameInfo::from)
                .collect(Collectors.toMap(GameInfo::gameId, Function.identity()));
    }

    /**
     * 해당 날짜(00:00 이상 다음날 00:00 미만)에 시작하는 경기 ID 를 조회한다.
     * 호출자가 경기 시작 시각 컬럼에 의존하지 않도록 식별자만 준다.
     *
     * @param gameDate 경기 날짜
     * @return 경기 ID 목록, 없으면 빈 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getIdsStartingOn(LocalDate gameDate) {
        return gameRepository.findIdsStartingOn(gameDate);
    }
}
