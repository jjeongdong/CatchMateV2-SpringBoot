package com.back.catchmate.orchestration.enroll;

import com.back.catchmate.application.board.service.BoardService;
import com.back.catchmate.application.bookmark.service.BookmarkService;
import com.back.catchmate.application.chat.service.ChatRoomMemberService;
import com.back.catchmate.application.chat.service.ChatService;
import com.back.catchmate.application.enroll.event.EnrollNotificationEvent;
import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.notification.model.NotificationTemplate;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.enroll.dto.command.EnrollCreateCommand;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollApplicantResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCancelResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCountResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollCreateResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollDetailResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollReceiveResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollRejectResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollRequestResponse;
import com.back.catchmate.orchestration.enroll.dto.response.EnrollResponse;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollOrchestrator {
    private final ChatService chatService;
    private final UserService userService;
    private final BoardService boardService;
    private final EnrollService enrollService;
    private final BookmarkService bookmarkService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public EnrollCreateResponse createEnroll(EnrollCreateCommand command) {
        User applicant = userService.getUser(command.getUserId());
        Board board = boardService.getCompletedBoard(command.getBoardId());

        if (applicant.getId().equals(board.getUser().getId())) {
            throw new BaseException(ErrorCode.ENROLL_BAD_REQUEST);
        }

        Enroll savedEnroll = enrollService.createEnroll(applicant, board, command.getDescription());

        // FCM 알림 발송
        eventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_REQUEST,
                board.getUser(),
                applicant,
                board,
                "ENROLL_REQUEST"
        ));

        return EnrollCreateResponse.of(savedEnroll.getId());
    }

    @Transactional
    public EnrollDetailResponse getEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollService.getEnrollWithFetch(enrollId);
        Long applicantId = enroll.getUser().getId();
        Long writerId = enroll.getBoard().getUser().getId();

        if (!userId.equals(applicantId) && !userId.equals(writerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (enroll.isNewEnroll() && userId.equals(writerId)) {
            enroll.markAsRead();
            enrollService.updateEnroll(enroll);
        }

        return EnrollDetailResponse.from(enroll);
    }

    public PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size) {
        DomainPage<Enroll> enrollPage = enrollService.getEnrollListByUserId(userId, DomainPageable.of(page, size));
        Map<Long, Boolean> bookmarkMap = getBookmarkStatusMap(userId, enrollPage.getContent());

        List<EnrollRequestResponse> responses = enrollPage.getContent().stream()
                .map(enroll -> EnrollRequestResponse.from(
                        enroll,
                        bookmarkMap.getOrDefault(enroll.getBoard().getId(), false)
                ))
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollApplicantResponse> getEnrollReceiveListByBoardId(Long userId, Long boardId, int page, int size) {
        Board board = boardService.getBoard(boardId);
        if (!board.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        DomainPage<Enroll> enrollPage = enrollService.getEnrollListByBoardIdAndStatus(boardId, AcceptStatus.PENDING, DomainPageable.of(page, size));

        List<EnrollApplicantResponse> responses = enrollPage.getContent().stream()
                .map(EnrollApplicantResponse::from)
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size) {
        DomainPage<Long> boardIdPage = enrollService.getBoardIdsWithPendingEnrolls(userId, DomainPageable.of(page, size));
        List<Long> boardIds = boardIdPage.getContent();

        if (boardIds.isEmpty()) {
            return new PagedResponse<>(boardIdPage, Collections.emptyList());
        }

        List<Enroll> allEnrolls = enrollService.getEnrollListByBoardIds(boardIds);

        List<EnrollReceiveResponse> content = boardIds.stream()
                .map(boardId -> mapToEnrollReceiveResponse(boardId, allEnrolls))
                .filter(Objects::nonNull)
                .toList();

        return new PagedResponse<>(boardIdPage, content);
    }

    public EnrollCountResponse getEnrollPendingCount(Long userId) {
        long count = enrollService.getEnrollPendingCountByBoardWriter(userId);
        return EnrollCountResponse.of(count);
    }

    @Transactional
    public EnrollAcceptResponse updateEnrollAccept(Long userId, Long enrollId) {
        Enroll enroll = enrollService.getEnrollWithLock(enrollId);
        Board board = boardService.getBoardWithLock(enroll.getBoard().getId());

        // 비즈니스 로직
        board.increaseCurrentPerson();
        enroll.accept();

        // 게시글 현재 인원수 증가, 신청 수락에 대한 정보 변경 사항 저장
        boardService.updateBoard(board);
        enrollService.updateEnroll(enroll);

        // 채팅방 처리
        ChatRoom chatRoom = chatService.getOrCreateChatRoom(board);
        chatRoomMemberService.addMember(chatRoom, enroll.getUser());

        // FCM 알림 발송
        eventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_ACCEPT,
                enroll.getUser(),
                board.getUser(),
                board,
                "ENROLL_ACCEPTED"
        ));

        return EnrollAcceptResponse.of(enrollId);
    }

    @Transactional
    public EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId) {
        Enroll enroll = enrollService.getEnrollWithLock(enrollId);
        Board board = boardService.getBoardWithLock(enroll.getBoard().getId());

        // 비즈니스 로직
        enroll.reject();
        enrollService.updateEnroll(enroll);

        // FCM 알림 발송
        eventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_REJECT,
                enroll.getUser(),
                board.getUser(),
                board,
                "ENROLL_REJECTED"
        ));

        return EnrollRejectResponse.of(enrollId);
    }

    @Transactional
    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollService.getEnroll(enrollId);

        if (!enroll.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        enrollService.deleteEnroll(enroll);
        return EnrollCancelResponse.of(enrollId);
    }

    // --- Private Helper Methods ---
    private Map<Long, Boolean> getBookmarkStatusMap(Long userId, List<Enroll> enrolls) {
        Map<Long, Boolean> map = new HashMap<>();
        for (Enroll enroll : enrolls) {
            boolean isBookMarked = bookmarkService.isBookmarked(userId, enroll.getBoard().getId());
            map.put(enroll.getBoard().getId(), isBookMarked);
        }
        return map;
    }

    private EnrollReceiveResponse mapToEnrollReceiveResponse(Long boardId, List<Enroll> allEnrolls) {
        List<Enroll> enrolls = allEnrolls.stream()
                .filter(e -> e.getBoard().getId().equals(boardId))
                .toList();

        if (enrolls.isEmpty()) return null;

        Board board = enrolls.get(0).getBoard();
        BoardResponse boardResponse = BoardResponse.from(board, false);

        List<EnrollResponse> enrollList = enrolls.stream()
                .map(EnrollResponse::from)
                .collect(Collectors.toList());

        return EnrollReceiveResponse.of(boardResponse, enrollList);
    }
}
