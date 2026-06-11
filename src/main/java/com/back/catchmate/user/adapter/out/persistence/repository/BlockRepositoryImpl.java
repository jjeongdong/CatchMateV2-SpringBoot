package com.back.catchmate.user.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.BlockRepository;
import com.back.catchmate.user.adapter.out.persistence.entity.BlockEntity;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
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
    public Optional<Block> findByBlockerAndBlocked(User blocker, User blocked) {
        return jpaBlockRepository.findByBlockerAndBlocked(UserEntity.from(blocker), UserEntity.from(blocked))
                .map(BlockEntity::toModel);
    }

    @Override
    public Page<Block> findAllByBlockerId(Long blockerId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt") // 최신 차단순
        );

        Page<BlockEntity> entityPage = jpaBlockRepository.findAllByBlockerId(blockerId, pageable);

        List<Block> domains = entityPage.getContent().stream()
                .map(BlockEntity::toModel)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public List<Long> findAllBlockedUserIdsByBlocker(User user) {
        return jpaBlockRepository.findAllBlockedUserIdsByBlocker(user.getId());
    }

    @Override
    public boolean existsByBlockerAndBlocked(User blocker, User blocked) {
        return jpaBlockRepository.existsByBlockerAndBlocked(UserEntity.from(blocker), UserEntity.from(blocked));
    }

    @Override
    public void delete(Block block) {
        BlockEntity entity = BlockEntity.from(block);
        jpaBlockRepository.delete(entity);
    }
}
