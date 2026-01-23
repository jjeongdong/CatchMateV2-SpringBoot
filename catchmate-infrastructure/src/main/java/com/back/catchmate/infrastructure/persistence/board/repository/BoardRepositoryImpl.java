package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.repository.BoardRepository;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpaBoardRepository;

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
    public Optional<Board> findFirstByUserIdAndIsCompletedFalse(Long userId) {
        return jpaBoardRepository.findFirstByUserIdAndCompletedFalse(userId)
                .map(BoardEntity::toModel);
    }

    @Override
    public void delete(Board board) {
        BoardEntity entity = BoardEntity.from(board);
        jpaBoardRepository.delete(entity);
    }
}
