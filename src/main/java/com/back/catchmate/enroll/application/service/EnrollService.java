package com.back.catchmate.enroll.application.service;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.bookmark.application.service.BookmarkService;
import com.back.catchmate.chat.application.event.ChatRoomMemberJoinedEvent;
import com.back.catchmate.chat.application.service.ChatRoomMemberService;
import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.idempotency.IdempotencyPort;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollApplicantResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCountResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCreateResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollDetailResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollReceiveResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRejectResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRequestResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollResponse;
import com.back.catchmate.enroll.application.event.EnrollNotificationEvent;
import com.back.catchmate.enroll.application.port.in.EnrollUseCase;
import com.back.catchmate.enroll.application.port.out.EnrollRepository;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollService implements EnrollUseCase {

    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final BoardService boardService;
    private final BookmarkService bookmarkService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final IdempotencyPort idempotencyPort;

    @Value("${enroll.idempotency.ttl-seconds:10}")
    private long idempotencyTtlSeconds;

    @Transactional
    public EnrollCreateResponse createEnroll(EnrollCreateCommand command) {
        User applicant = userService.getUser(command.getUserId());
        Board board = boardService.getCompletedBoard(command.getBoardId());

        if (applicant.getId().equals(board.getUser().getId())) {
            throw new BaseException(ErrorCode.ENROLL_BAD_REQUEST);
        }

        Enroll savedEnroll = createEnroll(applicant, board, command.getDescription());

        // FCM 알림 발송
        applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_REQUEST,
                board.getUser(),
                applicant,
                board,
                "ENROLL_REQUEST",
                savedEnroll.getId()
        ));

        return EnrollCreateResponse.of(savedEnroll.getId());
    }

    @Transactional
    public EnrollDetailResponse getEnroll(Long userId, Long enrollId) {
        Enroll enroll = getEnrollWithFetch(enrollId);
        Long applicantId = enroll.getUser().getId();
        Long writerId = enroll.getBoard().getUser().getId();

        if (!userId.equals(applicantId) && !userId.equals(writerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (enroll.isNewEnroll() && userId.equals(writerId)) {
            enroll.markAsRead();
            updateEnroll(enroll);
        }

        return EnrollDetailResponse.from(enroll);
    }

    public PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size) {
        DomainPage<Enroll> enrollPage = getEnrollListByUserId(userId, DomainPageable.of(page, size));
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

        DomainPage<Enroll> enrollPage = getEnrollListByBoardIdAndStatus(boardId, AcceptStatus.PENDING, DomainPageable.of(page, size));

        List<EnrollApplicantResponse> responses = enrollPage.getContent().stream()
                .map(EnrollApplicantResponse::from)
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size) {
        DomainPage<Long> boardIdPage = getBoardIdsWithPendingEnrolls(userId, DomainPageable.of(page, size));
        List<Long> boardIds = boardIdPage.getContent();

        if (boardIds.isEmpty()) {
            return new PagedResponse<>(boardIdPage, Collections.emptyList());
        }

        List<Enroll> allEnrolls = getEnrollListByBoardIds(boardIds);

        List<EnrollReceiveResponse> content = boardIds.stream()
                .map(boardId -> mapToEnrollReceiveResponse(boardId, allEnrolls))
                .filter(Objects::nonNull)
                .toList();

        return new PagedResponse<>(boardIdPage, content);
    }

    public EnrollCountResponse getEnrollPendingCount(Long userId) {
        long count = getEnrollPendingCountByBoardWriter(userId);
        return EnrollCountResponse.of(count);
    }

    @Transactional
    public EnrollAcceptResponse updateEnrollAccept(Long userId, Long enrollId) {
        String idempotencyKey = "idempotent:enroll:accept:" + enrollId;
        if (!idempotencyPort.acquireIfAbsent(idempotencyKey, idempotencyTtlSeconds)) {
            throw new BaseException(ErrorCode.DUPLICATE_ENROLL_ACCEPT_REQUEST);
        }

        Enroll enroll = getEnroll(enrollId);
        Board board = boardService.getBoardWithLock(enroll.getBoard().getId());

        // 비즈니스 로직
        board.increaseCurrentPerson();
        enroll.accept();

        // 게시글 현재 인원수 증가, 신청 수락에 대한 정보 변경 사항 저장
        boardService.updateBoard(board);
        updateEnroll(enroll);

        // 채팅방 처리
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(board);
        chatRoomMemberService.addMember(chatRoom, enroll.getUser());
        applicationEventPublisher.publishEvent(ChatRoomMemberJoinedEvent.of(chatRoom.getId(), enroll.getUser()));

        // FCM 알림 발송
        applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_ACCEPT,
                enroll.getUser(),
                board.getUser(),
                board,
                "ENROLL_ACCEPTED",
                enrollId
        ));

        return EnrollAcceptResponse.of(enrollId);
    }

    @Transactional
    public EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId) {
        Enroll enroll = getEnroll(enrollId);
        Board board = boardService.getBoardWithLock(enroll.getBoard().getId());

        // 비즈니스 로직
        enroll.reject();
        updateEnroll(enroll);

        // FCM 알림 발송
        applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_REJECT,
                enroll.getUser(),
                board.getUser(),
                board,
                "ENROLL_REJECTED",
                enrollId
        ));

        return EnrollRejectResponse.of(enrollId);
    }

    @Transactional
    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        Enroll enroll = getEnroll(enrollId);
        User applicant = enroll.getUser();
        Board board = enroll.getBoard();

        if (!applicant.getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        deleteEnroll(enroll);

        // 게시글 작성자에게 신청 취소 알림 발송 (FCM + WebSocket)
        applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(
                NotificationTemplate.ENROLL_CANCEL,
                board.getUser(),
                applicant,
                board,
                "ENROLL_CANCEL",
                enrollId
        ));

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


    private final EnrollRepository enrollRepository;

    public Enroll createEnroll(User user, Board board, String description) {
        validateDuplicateEnroll(user, board);
        Enroll enroll = Enroll.createEnroll(user, board, description);
        return enrollRepository.save(enroll);
    }

    public Enroll getEnroll(Long enrollId) {
        return enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Enroll getEnrollWithFetch(Long enrollId) {
        return enrollRepository.findByIdWithFetch(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    public Optional<Enroll> findEnrollById(Long enrollId) {
        return enrollRepository.findById(enrollId);
    }

    public Optional<Enroll> findEnrollByUserAndBoard(User user, Board board) {
        return enrollRepository.findByUserAndBoard(user, board);
    }

    public DomainPage<Enroll> getEnrollListByUserId(Long userId, DomainPageable pageable) {
        return enrollRepository.findAllByUserId(userId, pageable);
    }

    public DomainPage<Enroll> getEnrollListByBoardIdAndStatus(Long boardId, AcceptStatus acceptStatus, DomainPageable pageable) {
        return enrollRepository.findAllByBoardIdAndStatus(boardId, acceptStatus, pageable);
    }

    public DomainPage<Long> getBoardIdsWithPendingEnrolls(Long userId, DomainPageable pageable) {
        return enrollRepository.findBoardIdsWithPendingEnrolls(userId, pageable);
    }

    public List<Enroll> getEnrollListByBoardIds(List<Long> boardIds) {
        return enrollRepository.findAllByBoardIds(boardIds);
    }

    public List<Enroll> getEnrollListByIds(List<Long> ids) {
        return enrollRepository.findAllByIds(ids);
    }

    public Map<Long, AcceptStatus> getAcceptStatusMapByIds(List<Long> ids) {
        return enrollRepository.findAcceptStatusMapByIds(ids);
    }

    public Optional<AcceptStatus> findAcceptStatusById(Long id) {
        return enrollRepository.findAcceptStatusById(id);
    }

    public long getEnrollPendingCountByBoardWriter(Long userId) {
        return enrollRepository.countByBoardWriterAndStatus(userId, AcceptStatus.PENDING);
    }

    public List<Enroll> getAcceptedEnrollsBetween(Long applicantId, Long boardOwnerId) {
        return enrollRepository.findAllByApplicantAndBoardOwnerAndStatus(applicantId, boardOwnerId, AcceptStatus.ACCEPTED);
    }

    public void updateEnroll(Enroll enroll) {
        enrollRepository.save(enroll);
    }

    public void deleteEnroll(Enroll enroll) {
        enrollRepository.delete(enroll);
    }

    private void validateDuplicateEnroll(User user, Board board) {
        enrollRepository.findByUserAndBoard(user, board)
                .ifPresent(existingEnroll -> {
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.PENDING) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_PENDING);
                    }
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.REJECTED) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_REJECTED);
                    }
                    if (existingEnroll.getAcceptStatus() == AcceptStatus.ACCEPTED) {
                        throw new BaseException(ErrorCode.ALREADY_ENROLL_ACCEPTED);
                    }
                });
    }
}
