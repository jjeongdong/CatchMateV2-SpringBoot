package com.back.catchmate.chat.adapter.out.persistence.repository;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.application.port.out.ChatRoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.chat.adapter.out.persistence.entity.ChatRoomEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {
    private final JpaChatRoomRepository jpaChatRoomRepository;

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
    public Optional<Long> findLastMessageSequenceById(Long id) {
        return jpaChatRoomRepository.findLastMessageSequenceById(id);
    }

    @Override
    public Page<ChatRoom> findAllByUserId(Long userId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ChatRoomEntity> entityPage = jpaChatRoomRepository.findAllByUserIdWithPaging(userId, pageable);

        List<ChatRoom> domains = entityPage.getContent().stream()
                .map(ChatRoomEntity::toModel)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public List<ChatRoom> findAllByUserId(Long userId) {
        return jpaChatRoomRepository.findAllByUserId(userId).stream()
                .map(ChatRoomEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public void updateMaxSequence(Long roomId, Long sequence) {
        jpaChatRoomRepository.updateMaxSequence(roomId, sequence);
    }

    @Override
    public void delete(ChatRoom chatRoom) {
        ChatRoomEntity entity = ChatRoomEntity.from(chatRoom);
        jpaChatRoomRepository.delete(entity);
    }
}
