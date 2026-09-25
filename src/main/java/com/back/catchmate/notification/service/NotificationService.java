package com.back.catchmate.notification.service;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.service.EnrollQueryService;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import com.back.catchmate.notification.dto.response.NotificationResponse;
import com.back.catchmate.notification.dto.response.UnreadNotificationResponse;
import com.back.catchmate.notification.entity.Notification;
import com.back.catchmate.notification.entity.enums.AlarmType;
import com.back.catchmate.notification.repository.NotificationRepository;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private static final DateTimeFormatter GAME_INFO_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NotificationRepository notificationRepository;
    private final ClubService clubService;
    private final GameService gameService;
    private final UserService userService;
    private final BoardService boardService;
    private final EnrollQueryService enrollQueryService;

    public NotificationResponse getNotification(Long userId, Long notificationId) {
        Notification notification = getNotificationOrThrow(notificationId);
        verifyOwner(notification, userId);

        String acceptStatus = null;
        if (notification.getType() == AlarmType.ENROLL && notification.getTargetId() != null) {
            acceptStatus = enrollQueryService
                    .findAcceptStatusById(notification.getTargetId())
                    .orElse(null);
        }

        UserSummary sender =
                notification.getSenderId() != null ? userService.getUserSummary(notification.getSenderId()) : null;
        String gameInfo = resolveGameInfo(notification.getBoardId());
        return NotificationResponse.from(notification, sender, acceptStatus, gameInfo);
    }

    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        // 정렬은 idx_notifications_user_created(user_id, created_at DESC) 인덱스와 짝을 이룬다.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findAllByUserId(userId, pageable);

        List<Long> enrollIds = notificationPage.getContent().stream()
                .filter(n -> n.getType() == AlarmType.ENROLL && n.getTargetId() != null)
                .map(Notification::getTargetId)
                .toList();

        Map<Long, String> enrollStatusMap = enrollQueryService.getAcceptStatusMapByIds(enrollIds);

        Map<Long, String> gameInfoByBoardId = resolveGameInfos(notificationPage.getContent());

        List<Long> senderIds = notificationPage.getContent().stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserSummary> senderById = senderIds.isEmpty()
                ? Map.of()
                : userService.getUserSummaries(senderIds).stream()
                        .collect(Collectors.toMap(UserSummary::userId, Function.identity()));

        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notification -> {
                    String status = (notification.getType() == AlarmType.ENROLL)
                            ? enrollStatusMap.get(notification.getTargetId())
                            : null;
                    String gameInfo =
                            notification.getBoardId() != null ? gameInfoByBoardId.get(notification.getBoardId()) : null;
                    UserSummary sender =
                            notification.getSenderId() != null ? senderById.get(notification.getSenderId()) : null;
                    return NotificationResponse.from(notification, sender, status, gameInfo);
                })
                .toList();

        return new PagedResponse<>(notificationPage, responses);
    }

    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = notificationRepository.existsByUserIdAndRead(userId, false);
        return UnreadNotificationResponse.of(hasUnread);
    }

    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = getNotificationOrThrow(notificationId);
        verifyOwner(notification, userId);
        if (notification.isRead()) return;
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = getNotificationOrThrow(notificationId);
        verifyOwner(notification, userId);
        notificationRepository.delete(notification);
    }

    @Transactional
    public int readAllNotifications(Long userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void createNotification(
            Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        Notification notification = Notification.createNotification(userId, senderId, boardId, title, type, targetId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotifications(
            List<Long> userIds, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        if (userIds.isEmpty()) {
            return;
        }
        List<Notification> notifications = userIds.stream()
                .map(userId -> Notification.createNotification(userId, senderId, boardId, title, type, targetId))
                .toList();
        notificationRepository.saveAllInBatch(notifications);
    }

    private Notification getNotificationOrThrow(Long notificationId) {
        return notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private void verifyOwner(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private String resolveGameInfo(Long boardId) {
        if (boardId == null) {
            return null;
        }
        BoardSummary board = boardService.getBoardSummary(boardId);
        if (board == null || board.gameId() == null) {
            return null;
        }
        GameSummary game = gameService.getGameSummary(board.gameId());
        if (game == null) return null;
        ClubSummary homeClub = game.homeClubId() != null ? clubService.getClubSummary(game.homeClubId()) : null;
        ClubSummary awayClub = game.awayClubId() != null ? clubService.getClubSummary(game.awayClubId()) : null;
        return formatGameInfo(game, homeClub, awayClub);
    }

    private Map<Long, String> resolveGameInfos(List<Notification> notifications) {
        List<Long> boardIds = notifications.stream()
                .map(Notification::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BoardSummary> boardById = boardService.getBoardSummaries(boardIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BoardSummary::boardId, Function.identity()));

        List<Long> gameIds = boardById.values().stream()
                .map(BoardSummary::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, GameSummary> gameById = gameService.getGameSummaries(gameIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(GameSummary::gameId, Function.identity()));

        List<Long> clubIds = gameById.values().stream()
                .flatMap(g -> Stream.of(g.homeClubId(), g.awayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubSummary> clubById = clubIds.isEmpty()
                ? Map.of()
                : clubService.getClubSummaries(clubIds).stream()
                        .collect(Collectors.toMap(ClubSummary::clubId, Function.identity()));

        return boardIds.stream().collect(Collectors.toMap(Function.identity(), bid -> {
            BoardSummary b = boardById.get(bid);
            if (b == null || b.gameId() == null) return "";
            GameSummary game = gameById.get(b.gameId());
            if (game == null) return "";
            ClubSummary home = game.homeClubId() != null ? clubById.get(game.homeClubId()) : null;
            ClubSummary away = game.awayClubId() != null ? clubById.get(game.awayClubId()) : null;
            return formatGameInfo(game, home, away);
        }));
    }

    private static String formatGameInfo(GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        if (game == null || game.gameStartDate() == null) return null;
        String home = homeClub != null ? homeClub.name() : "?";
        String away = awayClub != null ? awayClub.name() : "?";
        return String.format(
                "%s · %s · %s vs %s",
                game.gameStartDate().format(GAME_INFO_FORMATTER),
                game.location() != null ? game.location() : "?",
                home,
                away);
    }
}
