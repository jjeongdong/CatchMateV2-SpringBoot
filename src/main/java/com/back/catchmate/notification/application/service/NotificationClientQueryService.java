package com.back.catchmate.notification.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;
import com.back.catchmate.notification.application.port.in.NotificationClientQueryUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationBoardInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationClubInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationGameInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.BoardFetchPort;
import com.back.catchmate.notification.application.port.out.external.ClubFetchPort;
import com.back.catchmate.notification.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.notification.application.port.out.external.GameFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.notification.domain.model.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationClientQueryService implements NotificationClientQueryUseCase {
    private static final DateTimeFormatter GAME_INFO_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private final NotificationReader notificationReader;
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;
    private final EnrollFetchPort enrollFetchPort;

    @Override
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        Notification notification = notificationReader.getNotification(notificationId);
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        String acceptStatus = null;
        if (notification.getType() == AlarmType.ENROLL && notification.getTargetId() != null) {
            acceptStatus = enrollFetchPort.findAcceptStatusById(notification.getTargetId())
                    .orElse(null);
        }

        NotificationUserInfo sender = notification.getSenderId() != null ? userFetchPort.getUser(notification.getSenderId()) : null;
        String gameInfo = resolveGameInfo(notification.getBoardId());
        return NotificationResponse.from(notification, sender, acceptStatus, gameInfo);
    }

    @Override
    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        Pageable domainPageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationReader.getNotificationList(userId, domainPageable);

        List<Long> enrollIds = notificationPage.getContent().stream()
                .filter(n -> n.getType() == AlarmType.ENROLL && n.getTargetId() != null)
                .map(Notification::getTargetId)
                .toList();

        Map<Long, String> enrollStatusMap = enrollFetchPort.getAcceptStatusMapByIds(enrollIds);

        Map<Long, String> gameInfoByBoardId = resolveGameInfos(notificationPage.getContent());

        List<Long> senderIds = notificationPage.getContent().stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, NotificationUserInfo> senderById = senderIds.isEmpty()
                ? Map.of()
                : userFetchPort.getUsers(senderIds).stream()
                        .collect(Collectors.toMap(NotificationUserInfo::userId, Function.identity()));

        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notification -> {
                    String status = (notification.getType() == AlarmType.ENROLL)
                            ? enrollStatusMap.get(notification.getTargetId())
                            : null;
                    String gameInfo = notification.getBoardId() != null
                            ? gameInfoByBoardId.get(notification.getBoardId())
                            : null;
                    NotificationUserInfo sender = notification.getSenderId() != null ? senderById.get(notification.getSenderId()) : null;
                    return NotificationResponse.from(notification, sender, status, gameInfo);
                })
                .toList();

        return new PagedResponse<>(notificationPage, responses);
    }

    @Override
    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = notificationReader.hasUnreadNotifications(userId);
        return UnreadNotificationResponse.of(hasUnread);
    }

    private String resolveGameInfo(Long boardId) {
        if (boardId == null) {
            return null;
        }
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        if (board == null || board.gameId() == null) {
            return null;
        }
        NotificationGameInfo game = gameFetchPort.getGame(board.gameId());
        if (game == null) return null;
        NotificationClubInfo homeClub = game.homeClubId() != null ? clubFetchPort.getClub(game.homeClubId()) : null;
        NotificationClubInfo awayClub = game.awayClubId() != null ? clubFetchPort.getClub(game.awayClubId()) : null;
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
        Map<Long, NotificationBoardInfo> boardById = boardFetchPort.getBoards(boardIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(NotificationBoardInfo::boardId, Function.identity()));

        List<Long> gameIds = boardById.values().stream()
                .map(NotificationBoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, NotificationGameInfo> gameById = gameFetchPort.getGames(gameIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(NotificationGameInfo::gameId, Function.identity()));

        List<Long> clubIds = gameById.values().stream()
                .flatMap(g -> Stream.of(g.homeClubId(), g.awayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, NotificationClubInfo> clubById = clubIds.isEmpty()
                ? Map.of()
                : clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(NotificationClubInfo::clubId, Function.identity()));

        return boardIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        bid -> {
                            NotificationBoardInfo b = boardById.get(bid);
                            if (b == null || b.gameId() == null) return "";
                            NotificationGameInfo game = gameById.get(b.gameId());
                            if (game == null) return "";
                            NotificationClubInfo home = game.homeClubId() != null ? clubById.get(game.homeClubId()) : null;
                            NotificationClubInfo away = game.awayClubId() != null ? clubById.get(game.awayClubId()) : null;
                            return formatGameInfo(game, home, away);
                        }
                ));
    }

    private static String formatGameInfo(NotificationGameInfo game, NotificationClubInfo homeClub, NotificationClubInfo awayClub) {
        if (game == null || game.gameStartDate() == null) return null;
        String home = homeClub != null ? homeClub.name() : "?";
        String away = awayClub != null ? awayClub.name() : "?";
        return String.format("%s · %s · %s vs %s",
                game.gameStartDate().format(GAME_INFO_FORMATTER),
                game.location() != null ? game.location() : "?",
                home,
                away
        );
    }
}
