package com.back.catchmate.infrastructure.persistence.enroll.repository;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.repository.EnrollRepository;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.infrastructure.persistence.enroll.entity.EnrollEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EnrollRepositoryImpl implements EnrollRepository {
    private final JpaEnrollRepository jpaEnrollRepository;

    @Override
    public Enroll save(Enroll enroll) {
        EnrollEntity entity = EnrollEntity.from(enroll);
        return jpaEnrollRepository.save(entity).toModel();
    }

    @Override
    public Optional<Enroll> findById(Long id) {
        return jpaEnrollRepository.findById(id)
                .map(EnrollEntity::toModel);
    }

    @Override
    public Optional<Enroll> findByIdWithLock(Long id) {
        return jpaEnrollRepository.findByIdWithPessimisticLock(id)
                .map(EnrollEntity::toModel);
    }

    @Override
    public Optional<Enroll> findByUserAndBoard(User user, Board board) {
        return jpaEnrollRepository.findByUserIdAndBoardId(user.getId(), board.getId())
                .map(EnrollEntity::toModel);
    }

    @Override
    public DomainPage<Enroll> findAllByUserId(Long userId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EnrollEntity> entityPage = jpaEnrollRepository.findAllByUserId(userId, pageable);

        List<Enroll> domains = entityPage.getContent().stream()
                .map(EnrollEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public DomainPage<Enroll> findAllByBoardIdAndStatus(Long boardId, AcceptStatus status, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EnrollEntity> entityPage = jpaEnrollRepository.findAllByBoardId(boardId, status, pageable);

        List<Enroll> domains = entityPage.getContent().stream()
                .map(EnrollEntity::toModel)
                .collect(Collectors.toList());

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public DomainPage<Long> findBoardIdsWithPendingEnrolls(Long userId, DomainPageable pageable) {
        PageRequest springPageable = PageRequest.of(
                pageable.getPage(),
                pageable.getSize()
        );

        Page<Long> idPage = jpaEnrollRepository.findDistinctBoardIdsByUserIdAndStatus(
                userId, AcceptStatus.PENDING, springPageable
        );

        return new DomainPage<>(
                idPage.getContent(),
                idPage.getNumber(),
                idPage.getSize(),
                idPage.getTotalElements()
        );
    }

    @Override
    public List<Enroll> findAllByBoardIds(List<Long> boardIds) {
        if (boardIds.isEmpty()) return Collections.emptyList();

        return jpaEnrollRepository.findAllByBoardIdInAndStatus(boardIds, AcceptStatus.PENDING)
                .stream()
                .map(EnrollEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Enroll> findByIdWithFetch(Long id) {
        return jpaEnrollRepository.findByIdWithFetch(id)
                .map(EnrollEntity::toModel);
    }

    @Override
    public long countByBoardWriterAndStatus(Long userId, AcceptStatus status) {
        return jpaEnrollRepository.countByBoardUserIdAndAcceptStatus(userId, status);
    }

    @Override
    public void delete(Enroll enroll) {
        jpaEnrollRepository.deleteById(enroll.getId());
    }
}
