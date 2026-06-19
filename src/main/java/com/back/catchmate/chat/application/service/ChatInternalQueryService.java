package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.response.ChatRecipientInternalResponse;
import com.back.catchmate.chat.application.port.in.ChatInternalQueryUseCase;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomRepository;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatInternalQueryService implements ChatInternalQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberService chatRoomMemberService;

    @Override
    public Optional<Long> findChatRoomIdByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId).map(ChatRoom::getId);
    }

    @Override
    public List<ChatRecipientInternalResponse> getChatRoomRecipients(Long chatRoomId, Long excludeUserId) {
        return chatRoomMemberService.getChatRoomMembers(chatRoomId).stream()
                .filter(member -> !member.getUserId().equals(excludeUserId))
                .map(member -> new ChatRecipientInternalResponse(member.getUserId(), member.isNotificationOn()))
                .toList();
    }
}
