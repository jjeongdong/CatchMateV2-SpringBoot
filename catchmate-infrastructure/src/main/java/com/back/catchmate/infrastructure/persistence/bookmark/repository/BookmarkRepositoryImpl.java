package com.back.catchmate.infrastructure.persistence.bookmark.repository;

import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.bookmark.repository.BookmarkRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.infrastructure.persistence.bookmark.entity.BookmarkEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        return jpaBookmarkRepository.save(entity).toModel();
    }

    @Override
    public Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId) {
        return jpaBookmarkRepository.findByUserIdAndBoardId(userId, boardId)
                .map(BookmarkEntity::toModel);
    }

    @Override
    public DomainPage<Bookmark> findAllByUserId(Long userId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<BookmarkEntity> entityPage = jpaBookmarkRepository.findAllByUserId(userId, pageable);

        List<Bookmark> domains = entityPage.getContent().stream()
                .map(BookmarkEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
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
        BookmarkEntity entity = BookmarkEntity.from(bookmark);
        jpaBookmarkRepository.delete(entity);
    }
}
