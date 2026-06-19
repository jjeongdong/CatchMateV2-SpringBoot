package com.back.catchmate.notice.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.notice.application.port.out.persistence.NoticeRepository;
import com.back.catchmate.notice.adapter.out.persistence.entity.NoticeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {
    private final JpaNoticeRepository jpaNoticeRepository;

    @Override
    public Notice save(Notice notice) {
        NoticeEntity entity = NoticeEntity.from(notice);
        return jpaNoticeRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return jpaNoticeRepository.findById(id)
                .map(NoticeEntity::toDomain);
    }

    @Override
    public Page<Notice> findAll(Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NoticeEntity> entityPage = jpaNoticeRepository.findAll(sortedPageRequest);

        List<Notice> domains = entityPage.getContent().stream()
                .map(NoticeEntity::toDomain)
                .toList();

        return new PageImpl<>(domains, sortedPageRequest, entityPage.getTotalElements());
    }

    @Override
    public void delete(Notice notice) {
        NoticeEntity entity = NoticeEntity.from(notice);
        jpaNoticeRepository.delete(entity);
    }
}
