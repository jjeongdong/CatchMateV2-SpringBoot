package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryDSLBoardRepository {
    Page<BoardEntity> findAllByCondition(BoardSearchCondition condition, Pageable pageable);
}
