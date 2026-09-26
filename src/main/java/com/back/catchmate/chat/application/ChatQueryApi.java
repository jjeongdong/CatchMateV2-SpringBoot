package com.back.catchmate.chat.application;

import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.repository.ChatRoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// chat 은 미전환이라 과도기 규칙대로 이 계약만 먼저 둔다.
// ChatRoomService 는 board 를 조회하므로, 거치면 board ↔ chat 빈 순환이 된다. 리포지토리를 직접 쓴다.
@Service
@RequiredArgsConstructor
public class ChatQueryApi {
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 게시글에 딸린 채팅방 ID 를 찾는다.
     *
     * @param boardId 게시글 ID
     * @return 채팅방 ID, 채팅방이 없으면 empty
     */
    @Transactional(readOnly = true)
    public Optional<Long> findChatRoomIdByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId).map(ChatRoom::getId);
    }
}
