package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.repository.BoardRepository;
import com.back.catchmate.domain.common.page.CursorPage;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final QueryDSLBoardRepository queryDSLBoardRepository;
    private final EntityManager entityManager;

    @Override
    public Board save(Board board) {
        BoardEntity entity = BoardEntity.from(board);
        return jpaBoardRepository.save(entity).toModel();
    }

    @Override
    public Optional<Board> findById(Long id) {
        return jpaBoardRepository.findById(id)
                .map(BoardEntity::toModel);
    }

    @Override
    public Optional<Board> findByIdWithLock(Long id) {
        return jpaBoardRepository.findByIdWithPessimisticLock(id)
                .map(entity -> {
                    entityManager.refresh(entity);
                    return entity.toModel();
                });
    }

    @Override
    public Optional<Board> findCompletedById(Long id) {
        return jpaBoardRepository.findByIdAndCompletedTrue(id)
                .map(BoardEntity::toModel);
    }

    @Override
    public Optional<Board> findTempBoardByUserId(Long userId) {
        return jpaBoardRepository.findFirstByUserIdAndCompletedFalse(userId)
                .map(BoardEntity::toModel);
    }

    @Override
    public DomainPage<Board> findAll(DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(domainPageable.getPage(), domainPageable.getSize());

        Page<BoardEntity> entityPage = jpaBoardRepository.findAllByCompletedTrue(pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public DomainPage<Board> findAllByCondition(BoardSearchCondition condition, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(domainPageable.getPage(), domainPageable.getSize());

        Page<BoardEntity> entityPage = queryDSLBoardRepository.findAllByCondition(condition, pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size) {
        List<BoardEntity> entities =
                queryDSLBoardRepository.findAllByConditionWithCursor(condition, size + 1);

        boolean hasNext = entities.size() > size;
        if (hasNext) {
            entities = new ArrayList<>(entities);
            entities.remove(size);
        }

        List<Board> domains = entities.stream()
                .map(BoardEntity::toModel)
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
    public DomainPage<Board> findAllByUserId(Long userId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by("liftUpDate").descending()
        );

        Page<BoardEntity> entityPage = jpaBoardRepository.findAllByUserId(userId, pageable);

        List<Board> domains = entityPage.getContent().stream()
                .map(BoardEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public long count() {
        return jpaBoardRepository.count();
    }

    @Override
    public void delete(Board board) {
        BoardEntity entity = BoardEntity.from(board);
        jpaBoardRepository.delete(entity);
    }
}
