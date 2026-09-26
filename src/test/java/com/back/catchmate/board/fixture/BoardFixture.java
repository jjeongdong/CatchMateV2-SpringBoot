package com.back.catchmate.board.fixture;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.PreferredAgeRange;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class BoardFixture {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0);

    private BoardFixture() {}

    // cheerClubId 1, gameId 100 인 발행 글
    public static Board published(Long boardId, Long writerId) {
        Board board = Board.create(
                writerId, "같이 직관가요", "3루 응원석", 4, 1L, 100L, true, "F", PreferredAgeRange.of(List.of("20대")), true, NOW);
        // 저장 없이 쓰는 단위 테스트용이라 id·생성 시각을 리플렉션으로 채운다.
        ReflectionTestUtils.setField(board, "id", boardId);
        ReflectionTestUtils.setField(board, "createdAt", NOW);
        return board;
    }

    public static Board draft(Long boardId, Long writerId) {
        Board board = Board.create(writerId, "임시", "", 4, null, null, false, null, null, false, NOW);
        ReflectionTestUtils.setField(board, "id", boardId);
        ReflectionTestUtils.setField(board, "createdAt", NOW);
        return board;
    }

    public static BoardInfo info(Long boardId, Long writerId) {
        return BoardInfo.from(published(boardId, writerId));
    }
}
