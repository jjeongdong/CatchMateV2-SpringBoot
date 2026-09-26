package com.back.catchmate.board.application;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardQueryApi {
    private final BoardRepository boardRepository;

    /**
     * 게시글 하나를 조회한다 (임시저장 글 포함, 삭제 글 제외).
     *
     * @param boardId 게시글 ID
     * @return 게시글 정보
     * @throws com.back.catchmate.board.domain.exception.BoardNotFoundException 없으면
     */
    @Transactional(readOnly = true)
    public BoardInfo getInfo(Long boardId) {
        return BoardInfo.from(boardRepository.getById(boardId));
    }

    /**
     * 발행된 게시글만 조회한다 (직관 신청 대상 확인용).
     *
     * @param boardId 게시글 ID
     * @return 게시글 정보
     * @throws com.back.catchmate.board.domain.exception.BoardNotFoundException 없거나 임시저장 글이면
     */
    @Transactional(readOnly = true)
    public BoardInfo getPublishedInfo(Long boardId) {
        return BoardInfo.from(boardRepository.getPublishedById(boardId));
    }

    /**
     * 여러 게시글을 한 번에 조회한다.
     *
     * @param boardIds 게시글 ID 들
     * @return 게시글 ID → 정보. 없는(삭제된) ID 는 빠진다. 입력이 비면 조회 없이 빈 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, BoardInfo> getInfos(Collection<Long> boardIds) {
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        return boardRepository.findAllByIds(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, BoardInfo::from, (first, second) -> first));
    }

    /**
     * 삭제되지 않은 게시글 수를 센다 (관리자 대시보드용, 임시저장 포함).
     *
     * @return 게시글 수
     */
    @Transactional(readOnly = true)
    public long count() {
        return boardRepository.count();
    }
}
