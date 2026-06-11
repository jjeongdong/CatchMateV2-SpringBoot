package com.back.catchmate.board.adapter.out.persistence.repository;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.application.port.out.BoardRepository;
import com.back.catchmate.common.page.CursorPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.board.adapter.out.persistence.entity.BoardEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpaBoardRepository;
    private final QueryDslBoardRepository queryDslBoardRepository;
    private final EntityManager entityManager;

    @Override
    public Board save(Board board) {
        BoardEntity entity = BoardEntity.fromDomain(board);
        return jpaBoardRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Board> findById(Long id) {
        return jpaBoardRepository.findById(id)
                .map(BoardEntity::toDomain);
    }

    @Override
    public Optional<Board> findByIdWithLock(Long id) {
        return jpaBoardRepository.findByIdWithPessimisticLock(id)
                .map(entity -> {
                    entityManager.refresh(entity);
                    return entity.toDomain();
                });
    }

    @Override
    public Optional<Board> findCompletedById(Long id) {
        return jpaBoardRepository.findByIdAndCompletedTrue(id)
                .map(BoardEntity::toDomain);
    }

    @Override
    public Optional<Board> findTempBoardByUserId(Long userId) {
        return jpaBoardRepository.findFirstByUserIdAndCompletedFalse(userId)
                .map(BoardEntity::toDomain);
    }

    @Override
    public Page<Board> findAll(Pageable domainPageable) {
        Pageable pageable = PageRequest.of(domainPageable.getPageNumber(), domainPageable.getPageSize());

        Page<BoardEntity> entityPage = jpaBoardRepository.findAllByCompletedTrue(pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toDomain)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public Page<Board> findAllByCondition(BoardSearchCondition condition, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(domainPageable.getPageNumber(), domainPageable.getPageSize());

        Page<BoardEntity> entityPage = queryDslBoardRepository.findAllByCondition(condition, pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toDomain)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size) {
        List<BoardEntity> entities =
                queryDslBoardRepository.findAllByConditionWithCursor(condition, size + 1);

        boolean hasNext = entities.size() > size;
        if (hasNext) {
            entities = new ArrayList<>(entities);
            entities.remove(size);
        }

        List<Board> domains = entities.stream()
                .map(BoardEntity::toDomain)
                .toList();

        Long nextCursorId = null;
        LocalDateTime nextCursorDateTime = null;
        if (hasNext && !domains.isEmpty()) {
            Board last = domains.get(domains.size() - 1);
            nextCursorId = last.getId();
            nextCursorDateTime = last.getLiftUpDate();
        }

        return new CursorPage<>(domains, hasNext, nextCursorId, nextCursorDateTime);
    }

    @Override
    public Page<Board> findAllByUserId(Long userId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by("liftUpDate").descending()
        );

        Page<BoardEntity> entityPage = jpaBoardRepository.findAllByUserId(userId, pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toDomain)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public long count() {
        return jpaBoardRepository.count();
    }

    @Override
    public void delete(Board board) {
        BoardEntity entity = BoardEntity.fromDomain(board);
        jpaBoardRepository.delete(entity);
    }
}
