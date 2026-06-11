package com.back.catchmate.enroll.adapter.out.persistence.repository;

import com.back.catchmate.board.domain.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.enroll.application.port.out.EnrollRepository;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.enroll.adapter.out.persistence.entity.EnrollEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Optional<Enroll> findByUserAndBoard(User user, Board board) {
        return jpaEnrollRepository.findByUserIdAndBoardId(user.getId(), board.getId())
                .map(EnrollEntity::toModel);
    }

    @Override
    public Page<Enroll> findAllByUserId(Long userId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EnrollEntity> entityPage = jpaEnrollRepository.findAllByUserId(userId, pageable);

        List<Enroll> domains = entityPage.getContent().stream()
                .map(EnrollEntity::toModel)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public Page<Enroll> findAllByBoardIdAndStatus(Long boardId, AcceptStatus status, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EnrollEntity> entityPage = jpaEnrollRepository.findAllByBoardId(boardId, status, pageable);

        List<Enroll> domains = entityPage.getContent().stream()
                .map(EnrollEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public Page<Long> findBoardIdsWithPendingEnrolls(Long userId, Pageable pageable) {
        PageRequest springPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Long> idPage = jpaEnrollRepository.findDistinctBoardIdsByUserIdAndStatus(
                userId, AcceptStatus.PENDING, springPageable
        );

        return new PageImpl<>(idPage.getContent(), pageable, idPage.getTotalElements());
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
    public List<Enroll> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return jpaEnrollRepository.findAllById(ids).stream()
                .map(EnrollEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, AcceptStatus> findAcceptStatusMapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        Map<Long, AcceptStatus> result = new HashMap<>();
        for (Object[] row : jpaEnrollRepository.findIdAndAcceptStatusByIdIn(ids)) {
            result.put((Long) row[0], (AcceptStatus) row[1]);
        }
        return result;
    }

    @Override
    public Optional<AcceptStatus> findAcceptStatusById(Long id) {
        return jpaEnrollRepository.findAcceptStatusById(id);
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
    public List<Enroll> findAllByApplicantAndBoardOwnerAndStatus(Long applicantId, Long ownerId, AcceptStatus status) {
        return jpaEnrollRepository.findAllByApplicantIdAndBoardOwnerIdAndStatus(applicantId, ownerId, status)
                .stream()
                .map(EnrollEntity::toModel)
                .toList();
    }

    @Override
    public void delete(Enroll enroll) {
        jpaEnrollRepository.deleteById(enroll.getId());
    }
}
