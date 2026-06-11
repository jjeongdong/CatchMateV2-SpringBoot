package com.back.catchmate.notice.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.notice.application.port.out.NoticeRepository;
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
        return jpaNoticeRepository.save(entity).toModel();
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return jpaNoticeRepository.findByIdWithWriter(id)
                .map(NoticeEntity::toModel);
    }

    @Override
    public Page<Notice> findAll(Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 페이징 조회 시에도 작성자 정보를 한 번에 가져옴 (Fetch Join)
        Page<NoticeEntity> entityPage = jpaNoticeRepository.findAllWithWriter(pageable);

        List<Notice> domains = entityPage.getContent().stream()
                .map(NoticeEntity::toModel)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public void delete(Notice notice) {
        NoticeEntity entity = NoticeEntity.from(notice);
        jpaNoticeRepository.delete(entity);
    }
}
