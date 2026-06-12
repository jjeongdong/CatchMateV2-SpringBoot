package com.back.catchmate.user.adapter.out.persistence.repository;

import com.back.catchmate.user.adapter.out.persistence.entity.BlockEntity;
import com.back.catchmate.user.application.port.out.BlockRepository;
import com.back.catchmate.user.domain.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    public Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return jpaBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .map(BlockEntity::toModel);
    }

    @Override
    public Page<Block> findAllByBlockerId(Long blockerId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<BlockEntity> entityPage = jpaBlockRepository.findAllByBlockerId(blockerId, pageable);

        List<Block> domains = entityPage.getContent().stream()
                .map(BlockEntity::toModel)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public List<Long> findAllBlockedUserIdsByBlockerId(Long blockerId) {
        return jpaBlockRepository.findAllBlockedUserIdsByBlockerId(blockerId);
    }

    @Override
    public boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return jpaBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public void delete(Block block) {
        BlockEntity entity = BlockEntity.from(block);
        jpaBlockRepository.delete(entity);
    }
}
