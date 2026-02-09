package com.back.catchmate.application.chat.service;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.repository.ChatRoomRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberService chatRoomMemberService;

    public ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    public Optional<ChatRoom> findById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId);
    }

    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId);
    }

    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 게시글로 채팅방 조회 또는 생성
     * 이미 채팅방이 있으면 기존 채팅방을 반환하고, 없으면 새로 생성
     */
    public ChatRoom getOrCreateChatRoom(Board board) {
        return chatRoomRepository.findByBoardId(board.getId())
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.createChatRoom(board);
                    return chatRoomRepository.save(newChatRoom);
                });
    }

    // 사용자 기준으로 참가중인 채팅방 리스트 조회 (페이징)
    // ChatRoomMember 테이블을 통해 활성 멤버의 채팅방만 조회
    public DomainPage<ChatRoom> findAllByUserId(Long userId, DomainPageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
    }

    // 사용자 기준으로 참가중인 채팅방 리스트 조회
    // ChatRoomMember 테이블을 통해 활성 멤버의 채팅방만 조회
    public List<ChatRoom> findAllByUserId(Long userId) {
        return chatRoomRepository.findAllByUserId(userId);
    }

    // 특정 채팅방에 사용자가 참여중인지 확인
    public boolean isUserParticipant(Long userId, Long chatRoomId) {
        return chatRoomMemberService.isActiveMember(chatRoomId, userId);
    }
}
