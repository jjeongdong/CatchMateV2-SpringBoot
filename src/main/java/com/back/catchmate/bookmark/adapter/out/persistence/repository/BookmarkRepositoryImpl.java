package com.back.catchmate.bookmark.adapter.out.persistence.repository;

import com.back.catchmate.bookmark.domain.model.Bookmark;
import com.back.catchmate.bookmark.application.port.out.persistence.BookmarkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.bookmark.adapter.out.persistence.entity.BookmarkEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final JpaBookmarkRepository jpaBookmarkRepository;

    @Override
    public Bookmark save(Bookmark bookmark) {
        BookmarkEntity entity = BookmarkEntity.from(bookmark);
        return jpaBookmarkRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId) {
        return jpaBookmarkRepository.findByUserIdAndBoardId(userId, boardId)
                .map(BookmarkEntity::toDomain);
    }

    @Override
    public Page<Bookmark> findAllByUserId(Long userId, Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<BookmarkEntity> entityPage = jpaBookmarkRepository.findAllByUserId(userId, sortedPageRequest);

        return entityPage.map(BookmarkEntity::toDomain);
    }

    @Override
    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return jpaBookmarkRepository.findBookmarkedBoardIds(userId, boardIds);
    }

    @Override
    public boolean existsByUserIdAndBoardId(Long userId, Long boardId) {
        return jpaBookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    @Override
    public void delete(Bookmark bookmark) {
        jpaBookmarkRepository.deleteById(bookmark.getId());
    }
}
