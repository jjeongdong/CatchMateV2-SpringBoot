package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.domain.chat.repository.ChatRoomRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {
    private final JpaChatRoomRepository jpaChatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomEntity entity = ChatRoomEntity.from(chatRoom);
        return jpaChatRoomRepository.save(entity).toModel();
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return jpaChatRoomRepository.findByIdWithBoard(id)
                .map(ChatRoomEntity::toModel);
    }

    @Override
    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return jpaChatRoomRepository.findByBoardId(boardId)
                .map(ChatRoomEntity::toModel);
    }

    @Override
    public DomainPage<ChatRoom> findAllByUserId(Long userId, DomainPageable domainPageable) {
        // 1. ChatRoomMember에서 사용자의 활성 채팅방 멤버 목록을 조회
        List<ChatRoomMember> activeMembers = chatRoomMemberRepository.findAllByUserIdAndActive(userId);

        // 2. 채팅방 ID 목록 추출
        List<Long> chatRoomIds = activeMembers.stream()
                .map(member -> member.getChatRoom().getId())
                .collect(Collectors.toList());

        // 3. 채팅방이 없으면 빈 페이지 반환
        if (chatRoomIds.isEmpty()) {
            return new DomainPage<>(
                    List.of(),
                    domainPageable.getPage(),
                    domainPageable.getSize(),
                    0L
            );
        }

        // 4. 채팅방 ID 목록으로 채팅방 조회 (페이징)
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ChatRoomEntity> entityPage = jpaChatRoomRepository.findAllByIdIn(chatRoomIds, pageable);

        List<ChatRoom> chatRooms = entityPage.getContent().stream()
                .map(ChatRoomEntity::toModel)
                .toList();

        return new DomainPage<>(
                chatRooms,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public List<ChatRoom> findAllByUserId(Long userId) {
        // ChatRoomMember에서 사용자의 활성 채팅방 목록을 조회
        List<ChatRoomMember> activeMembers = chatRoomMemberRepository.findAllByUserIdAndActive(userId);

        return activeMembers.stream()
                .map(ChatRoomMember::getChatRoom)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBoardId(Long boardId) {
        return jpaChatRoomRepository.existsByBoardId(boardId);
    }

    @Override
    public void delete(ChatRoom chatRoom) {
        ChatRoomEntity entity = ChatRoomEntity.from(chatRoom);
        jpaChatRoomRepository.delete(entity);
    }
}
