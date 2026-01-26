package com.back.catchmate.infrastructure.persistence.user.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.repository.BlockRepository;
import com.back.catchmate.infrastructure.persistence.user.entity.BlockEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
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
public class BlockRepositoryImpl implements BlockRepository {
    private final JpaBlockRepository jpaBlockRepository;

    @Override
    public Block save(Block block) {
        BlockEntity entity = BlockEntity.from(block);
        return jpaBlockRepository.save(entity).toModel();
    }

    @Override
    public void delete(Block block) {
        BlockEntity entity = BlockEntity.from(block);
        jpaBlockRepository.delete(entity);
    }

    @Override
    public boolean existsByBlockerAndBlocked(User blocker, User blocked) {
        return jpaBlockRepository.existsByBlockerAndBlocked(UserEntity.from(blocker), UserEntity.from(blocked));
    }

    @Override
    public Optional<Block> findByBlockerAndBlocked(User blocker, User blocked) {
        return jpaBlockRepository.findByBlockerAndBlocked(UserEntity.from(blocker), UserEntity.from(blocked))
                .map(BlockEntity::toModel);
    }

    @Override
    public DomainPage<Block> findAllByBlocker(Long blockerId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt") // 최신 차단순
        );

        Page<BlockEntity> entityPage = jpaBlockRepository.findAllByBlockerId(blockerId, pageable);

        List<Block> domains = entityPage.getContent().stream()
                .map(BlockEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public List<Long> findAllBlockedUserIdsByBlocker(User user) {
        return jpaBlockRepository.findAllBlockedUserIdsByBlocker(UserEntity.from(user));
    }
}
