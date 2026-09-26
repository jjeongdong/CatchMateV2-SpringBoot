package com.back.catchmate.bookmark.infrastructure;

import static com.back.catchmate.bookmark.domain.QBookmark.bookmark;

import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.bookmark.domain.BookmarkRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final BookmarkJpaRepository bookmarkJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void saveIfAbsent(Bookmark newBookmark) {
        // 유니크 위반을 잡아 무시하면 Hibernate 가 트랜잭션을 rollback-only 로 만들어 커밋이 실패한다. 먼저 확인한다.
        // 동시 중복 요청은 (user_id, board_id) 유니크 제약이 막는다.
        if (bookmarkJpaRepository.existsByUserIdAndBoardId(newBookmark.getUserId(), newBookmark.getBoardId())) {
            return;
        }
        bookmarkJpaRepository.save(newBookmark);
    }

    @Override
    public void deleteByUserIdAndBoardId(Long userId, Long boardId) {
        bookmarkJpaRepository.deleteByUserIdAndBoardId(userId, boardId);
    }

    @Override
    public boolean existsByUserIdAndBoardId(Long userId, Long boardId) {
        return bookmarkJpaRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    @Override
    public Set<Long> findBookmarkedBoardIds(Long userId, Collection<Long> boardIds) {
        return Set.copyOf(bookmarkJpaRepository.findBookmarkedBoardIds(userId, boardIds));
    }

    @Override
    public List<Bookmark> findAllByUserId(Long userId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(bookmark)
                .where(bookmark.userId.eq(userId))
                .orderBy(bookmark.createdAt.desc(), bookmark.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByUserId(Long userId) {
        return bookmarkJpaRepository.countByUserId(userId);
    }
}
