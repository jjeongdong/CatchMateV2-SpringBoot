package com.back.catchmate.notification.application.service;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;
import com.back.catchmate.notification.application.port.in.NotificationUseCase;
import com.back.catchmate.notification.application.port.out.ClubFetchPort;
import com.back.catchmate.notification.application.port.out.EnrollFetchPort;
import com.back.catchmate.notification.application.port.out.GameFetchPort;
import com.back.catchmate.notification.application.port.out.NotificationRepository;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private static final DateTimeFormatter GAME_INFO_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NotificationRepository notificationRepository;

    private final NotificationRetryService notificationRetryService;

    private final ClubFetchPort clubFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final GameFetchPort gameFetchPort;

    @Transactional
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        // 1. 서비스 호출 (조회 및 읽음 처리 로직 위임)
        Notification notification = getNotification(notificationId);

        AcceptStatus acceptStatus = null;
        if (notification.getType() == AlarmType.ENROLL && notification.getTargetId() != null) {
            acceptStatus = enrollFetchPort.findAcceptStatusById(notification.getTargetId())
                    .orElse(null);
        }

        String gameInfo = resolveGameInfo(notification.getBoard());
        // 2. DTO 변환 및 반환
        return NotificationResponse.from(notification, acceptStatus, gameInfo);
    }

    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);

        // 1. 서비스 호출
        Page<Notification> notificationPage = getNotificationList(userId, domainPageable);

        // 2. 신청 상태 정보 일괄 조회 (N+1 방지)
        List<Long> enrollIds = notificationPage.getContent().stream()
                .filter(n -> n.getType() == AlarmType.ENROLL && n.getTargetId() != null)
                .map(Notification::getTargetId)
                .toList();

        Map<Long, AcceptStatus> enrollStatusMap = enrollFetchPort.getAcceptStatusMapByIds(enrollIds);

        // 3. 게임 정보 일괄 조회
        Map<Long, String> gameInfoByBoardId = resolveGameInfos(notificationPage.getContent());

        // 4. DTO 변환
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notification -> {
                    AcceptStatus status = (notification.getType() == AlarmType.ENROLL)
                            ? enrollStatusMap.get(notification.getTargetId())
                            : null;
                    String gameInfo = notification.getBoard() != null
                            ? gameInfoByBoardId.get(notification.getBoard().getId())
                            : null;
                    return NotificationResponse.from(notification, status, gameInfo);
                })
                .toList();

        return new PagedResponse<>(notificationPage, responses);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        deleteNotificationEntity(userId, notificationId);
    }

    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = existsUnreadNotifications(userId);
        return UnreadNotificationResponse.of(hasUnread);
    }

    @Transactional
    public int readAllNotifications(Long userId) {
        return markAllRead(userId);
    }

    public void processPendingNotifications() {
        notificationRetryService.processPendingNotifications();
    }

    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    // 조회와 읽음 처리를 함께 수행하는 비즈니스 메서드
    public Notification getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }

        return notification;
    }

    public Page<Notification> getNotificationList(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    public void deleteNotificationEntity(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 소유권 검증 로직이 필요하다면 여기에 추가 (현재는 AOP에서 처리 중)
        notificationRepository.delete(notification);
    }

    public boolean existsUnreadNotifications(Long userId) {
        return notificationRepository.hasUnreadNotifications(userId);
    }

    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    // --- Helpers ---
    private String resolveGameInfo(Board board) {
        if (board == null || board.getGameId() == null) {
            return null;
        }
        Game game = gameFetchPort.getGame(board.getGameId());
        if (game == null) return null;
        Club homeClub = game.getHomeClubId() != null ? clubFetchPort.getClub(game.getHomeClubId()) : null;
        Club awayClub = game.getAwayClubId() != null ? clubFetchPort.getClub(game.getAwayClubId()) : null;
        return formatGameInfo(game, homeClub, awayClub);
    }

    private Map<Long, String> resolveGameInfos(List<Notification> notifications) {
        List<Board> boards = notifications.stream()
                .map(Notification::getBoard)
                .filter(Objects::nonNull)
                .toList();

        List<Long> gameIds = boards.stream()
                .map(Board::getGameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Game> gameById = gameFetchPort.getGames(gameIds).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));

        List<Long> clubIds = gameById.values().stream()
                .flatMap(g -> Stream.of(g.getHomeClubId(), g.getAwayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Club> clubById = clubIds.isEmpty()
                ? Map.of()
                : clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(Club::getId, Function.identity()));

        return boards.stream()
                .filter(b -> b.getGameId() != null)
                .collect(Collectors.toMap(
                        Board::getId,
                        b -> {
                            Game game = gameById.get(b.getGameId());
                            if (game == null) return "";
                            Club home = game.getHomeClubId() != null ? clubById.get(game.getHomeClubId()) : null;
                            Club away = game.getAwayClubId() != null ? clubById.get(game.getAwayClubId()) : null;
                            return formatGameInfo(game, home, away);
                        },
                        (a, b) -> a
                ));
    }

    private static String formatGameInfo(Game game, Club homeClub, Club awayClub) {
        if (game == null || game.getGameStartDate() == null) return null;
        String home = homeClub != null ? homeClub.getName() : "?";
        String away = awayClub != null ? awayClub.getName() : "?";
        return String.format("%s · %s · %s vs %s",
                game.getGameStartDate().format(GAME_INFO_FORMATTER),
                game.getLocation() != null ? game.getLocation() : "?",
                home,
                away
        );
    }
}
