package com.back.catchmate.application.enroll;

import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.enroll.dto.command.EnrollCreateCommand;
import com.back.catchmate.application.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollCountResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollDetailResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollReceiveResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollCancelResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollCreateResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollApplicantResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollRejectResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollRequestResponse;
import com.back.catchmate.application.enroll.dto.response.EnrollResponse;
import com.back.catchmate.application.enroll.event.EnrollNotificationEvent;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.bookmark.service.BookmarkService;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.service.ChatRoomMemberService;
import com.back.catchmate.domain.chat.service.ChatRoomService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.service.EnrollService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.service.NotificationService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import user.enums.AlarmType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollUseCase {
    private final EnrollService enrollService;
    private final BookmarkService bookmarkService;
    private final BoardService boardService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @Transactional
    public EnrollCreateResponse createEnroll(EnrollCreateCommand command) {
        // 신청자 정보 조회
        User applicant = userService.getUser(command.getUserId());

        // 게시글 정보 조회
        Board board = boardService.getCompletedBoard(command.getBoardId());

        if (applicant.getId().equals(board.getUser().getId())) {
            throw new BaseException(ErrorCode.ENROLL_BAD_REQUEST);
        }

        // 직관 신청 생성 및 저장
        Enroll savedEnroll = enrollService.createEnroll(applicant, board, command.getDescription());

        // 게시글 작성자에게 신청 알림 발송 및 저장
        saveNotification(
                board.getUser(),
                applicant,
                "직관 신청 알림",
                board.getId()
        );

        publishEnrollEvent(
                board.getUser(),
                board,
                "직관 신청 알림",
                String.format("%s님이 [%s] 직관을 신청했습니다.",
                        applicant.getNickName(), board.getTitle()),
                "ENROLL_REQUEST"
        );

        return EnrollCreateResponse.of(savedEnroll.getId());
    }

    @Transactional
    public EnrollDetailResponse getEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollService.getEnrollWithFetch(enrollId);

        Long applicantId = enroll.getUser().getId();
        Long writerId = enroll.getBoard().getUser().getId();

        // 요청한 유저(userId)가 신청자도 아니고, 작성자도 아니면 에러
        if (!userId.equals(applicantId) && !userId.equals(writerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (enroll.isNew() && userId.equals(writerId)) {
            enroll.markAsRead();
            enrollService.updateEnroll(enroll);
        }

        // DTO 변환
        return EnrollDetailResponse.from(enroll);
    }

    public PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size) {
        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);
        // 직관 신청 리스트 조회
        DomainPage<Enroll> enrollPage = enrollService.getEnrollListByUserId(userId, domainPageable);

        // DTO 변환
        List<EnrollRequestResponse> responses = enrollPage.getContent().stream()
                .map(enroll -> {
                    boolean isBookMarked = bookmarkService.isBookmarked(
                            userId,
                            enroll.getBoard().getId()
                    );
                    return EnrollRequestResponse.from(enroll, isBookMarked);
                })
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollApplicantResponse> getEnrollReceiveListByBoardId(Long userId, Long boardId, int page, int size) {
        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);
        Board board = boardService.getBoard(boardId);

        if (!board.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 직관 신청 리스트 조회
        DomainPage<Enroll> enrollPage = enrollService.getEnrollListByBoardIdAndStatus(boardId, AcceptStatus.PENDING, domainPageable);

        // DTO 변환
        List<EnrollApplicantResponse> responses = enrollPage.getContent().stream()
                .map(EnrollApplicantResponse::from)
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size) {
        DomainPageable pageable = DomainPageable.of(page, size);

        // 신청이 있는 게시글 ID 목록 조회
        DomainPage<Long> boardIdPage = enrollService.getBoardIdsWithPendingEnrolls(userId, pageable);
        List<Long> boardIds = boardIdPage.getContent();

        // 데이터가 없으면 빈 페이지 반환
        if (boardIds.isEmpty()) {
            return new PagedResponse<>(boardIdPage, Collections.emptyList());
        }

        // 위 게시글들에 속한 신청 전체 조회
        List<Enroll> allEnrolls = enrollService.getEnrollListByBoardIds(boardIds);

        // boardIds 순서를 지키면서 DTO 조립
        List<EnrollReceiveResponse> content = boardIds.stream()
                .map(boardId -> {
                    // 해당 게시글의 신청서만 필터링
                    List<Enroll> enrolls = allEnrolls.stream()
                            .filter(e -> e.getBoard().getId().equals(boardId))
                            .toList();

                    // 신청이 없으면 null 반환
                    if (enrolls.isEmpty()) return null;

                    // 게시글 DTO 변환
                    Board board = enrolls.get(0).getBoard();
                    BoardResponse boardResponse = BoardResponse.from(board, false);

                    // 신청자 목록 DTO 변환
                    List<EnrollResponse> enrollList = enrolls.stream()
                            .map(EnrollResponse::from)
                            .collect(Collectors.toList());

                    return EnrollReceiveResponse.of(boardResponse, enrollList);
                })
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
        // 1. 신청 내역 조회
        Enroll enroll = enrollService.getEnrollWithLock(enrollId);
        Board board = enroll.getBoard();

        // 2. 게시글 인원 확인 및 증가
        board.increaseCurrentPerson();

        // 3. 신청 상태 변경
        enroll.accept();

        // 4. 변경 사항 저장
        boardService.updateBoard(board);
        enrollService.updateEnroll(enroll);

        // 5. 채팅방에 신청자 추가
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(board);
        chatRoomMemberService.addMember(chatRoom, enroll.getUser());

        saveNotification(
                enroll.getUser(),
                board.getUser(),
                "직관 신청 수락 알림",
                board.getId()
        );

        publishEnrollEvent(
                enroll.getUser(),
                board,
                "직관 신청 수락 알림",
                String.format("[%s] 신청이 수락되었습니다. 채팅방을 확인해보세요!",
                        board.getTitle()),
                "ENROLL_ACCEPTED"
        );

        return EnrollAcceptResponse.of(enrollId);
    }

    @Transactional
    public EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId) {
        // 1. 신청 내역 조회
        Enroll enroll = enrollService.getEnrollWithLock(enrollId);
        Board board = enroll.getBoard();

        // 2. 신청 상태 변경
        enroll.reject();

        // 3. 변경 사항 저장
        enrollService.updateEnroll(enroll);

        saveNotification(
                enroll.getUser(),
                board.getUser(),
                "직관 신청 거절 알림",
                board.getId()
        );

        publishEnrollEvent(
                enroll.getUser(),
                board,
                "직관 신청 거절 알림",
                String.format("아쉽지만 [%s] 신청이 거절되었습니다.",
                        board.getTitle()),
                "ENROLL_REJECTED"
        );

        return EnrollRejectResponse.of(enrollId);
    }

    @Transactional
    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        // 직관 신청 조회
        Enroll enroll = enrollService.getEnroll(enrollId);

        // 권한 확인 (신청자 본인만 취소 가능)
        if (!enroll.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 직관 신청 취소
        enrollService.deleteEnroll(enroll);

        return EnrollCancelResponse.of(enrollId);
    }

    private void saveNotification(User user, User sender, String title, Long referenceId) {
        Notification notification = Notification.createNotification(
                user,
                sender,
                title,
                AlarmType.ENROLL,
                referenceId
        );
        notificationService.createNotification(notification);
    }

    private void publishEnrollEvent(User recipient, Board board, String title, String body, String type) {
        eventPublisher.publishEvent(
                new EnrollNotificationEvent(
                        recipient,
                        board,
                        title,
                        body,
                        type
                )
        );
    }
}
