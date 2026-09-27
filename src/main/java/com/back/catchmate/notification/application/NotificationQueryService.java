package com.back.catchmate.notification.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.EnrollQueryApi;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.notification.application.dto.result.NotificationResult;
import com.back.catchmate.notification.application.dto.result.NotificationUnreadResult;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private static final DateTimeFormatter GAME_INFO_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final String UNKNOWN = "?";

    private final NotificationRepository notificationRepository;
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;
    private final GameQueryApi gameQueryApi;
    private final ClubQueryApi clubQueryApi;
    private final EnrollQueryApi enrollQueryApi;

    @Transactional(readOnly = true)
    public NotificationResult getNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.getById(notificationId);
        notification.verifyOwner(userId);
        return toResults(List.of(notification)).get(0);
    }

    @Transactional(readOnly = true)
    public CursorPageResult<NotificationResult> getNotifications(Long userId, String cursor, int size) {
        NotificationCursor decoded = cursor != null ? NotificationCursor.decode(cursor) : null;
        List<Notification> rows = notificationRepository.findPageByUserId(
                userId,
                decoded != null ? decoded.createdAt() : null,
                decoded != null ? decoded.notificationId() : null,
                size + 1);

        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;
        String nextCursor =
                hasNext ? NotificationCursor.from(page.get(page.size() - 1)).encode() : null;
        return new CursorPageResult<>(toResults(page), nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public NotificationUnreadResult getUnread(Long userId) {
        return new NotificationUnreadResult(notificationRepository.existsUnreadByUserId(userId));
    }

    // 신청 상태·경기 정보·발신자를 BC 마다 한 번씩 모아 조합한다 (조회 수가 알림 수와 무관).
    private List<NotificationResult> toResults(List<Notification> notifications) {
        List<Long> enrollIds = notifications.stream()
                .filter(Notification::isEnroll)
                .map(Notification::getTargetId)
                .distinct()
                .toList();
        Map<Long, String> acceptStatusesByEnrollId =
                enrollIds.isEmpty() ? Map.of() : enrollQueryApi.getAcceptStatuses(enrollIds);

        Map<Long, String> gameInfosByBoardId = gameInfosByBoardId(notifications);

        List<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserInfo> sendersById = senderIds.isEmpty() ? Map.of() : userQueryApi.getInfos(senderIds);

        List<NotificationResult> results = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            results.add(NotificationResult.of(
                    notification,
                    notification.getSenderId() != null ? sendersById.get(notification.getSenderId()) : null,
                    notification.isEnroll() ? acceptStatusesByEnrollId.get(notification.getTargetId()) : null,
                    notification.getBoardId() != null ? gameInfosByBoardId.get(notification.getBoardId()) : null));
        }
        return results;
    }

    // 게시글이 삭제됐거나 경기를 알 수 없으면 빈 문자열이다 — 알림 조회가 그 때문에 실패하지 않게 한다.
    private Map<Long, String> gameInfosByBoardId(List<Notification> notifications) {
        List<Long> boardIds = notifications.stream()
                .map(Notification::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BoardInfo> boardsById = boardQueryApi.getInfos(boardIds);

        List<Long> gameIds = boardsById.values().stream()
                .map(BoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, GameInfo> gamesById = gameIds.isEmpty() ? Map.of() : gameQueryApi.getInfos(gameIds);

        List<Long> clubIds = new ArrayList<>();
        for (GameInfo game : gamesById.values()) {
            addIfAbsent(clubIds, game.homeClubId());
            addIfAbsent(clubIds, game.awayClubId());
        }
        Map<Long, ClubInfo> clubsById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);

        Map<Long, String> gameInfosByBoardId = new HashMap<>();
        for (Long boardId : boardIds) {
            BoardInfo board = boardsById.get(boardId);
            GameInfo game = board != null && board.gameId() != null ? gamesById.get(board.gameId()) : null;
            gameInfosByBoardId.put(boardId, formatGameInfo(game, clubsById));
        }
        return gameInfosByBoardId;
    }

    private static String formatGameInfo(GameInfo game, Map<Long, ClubInfo> clubsById) {
        if (game == null || game.gameStartDate() == null) {
            return "";
        }
        return String.format(
                "%s · %s · %s vs %s",
                game.gameStartDate().format(GAME_INFO_FORMATTER),
                game.location() != null ? game.location() : UNKNOWN,
                clubName(clubsById, game.homeClubId()),
                clubName(clubsById, game.awayClubId()));
    }

    private static String clubName(Map<Long, ClubInfo> clubsById, Long clubId) {
        ClubInfo club = clubId != null ? clubsById.get(clubId) : null;
        return club != null ? club.name() : UNKNOWN;
    }

    private static void addIfAbsent(List<Long> ids, Long id) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }
}
