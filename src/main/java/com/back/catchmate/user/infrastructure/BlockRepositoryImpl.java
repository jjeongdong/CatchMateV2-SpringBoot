package com.back.catchmate.user.infrastructure;

import static com.back.catchmate.user.domain.QBlock.block;

import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.exception.BlockAlreadyExistsException;
import com.back.catchmate.user.domain.exception.BlockNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlockRepositoryImpl implements BlockRepository {
    private final BlockJpaRepository blockJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Block save(Block newBlock) {
        // (blocker_id, blocked_id) 유니크 제약이 "이미 차단함" 규칙의 최종 방어선이다.
        // 선조회 대신 제약 위반을 변환해 동시 요청에도 중복 행이 생기지 않게 한다.
        // 두 컬럼 모두 NOT NULL 이고 항상 채워지므로 이 테이블에서 날 수 있는 무결성 위반은 유니크 제약뿐이다.
        try {
            return blockJpaRepository.saveAndFlush(newBlock);
        } catch (DataIntegrityViolationException e) {
            throw new BlockAlreadyExistsException();
        }
    }

    @Override
    public Block getByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return blockJpaRepository
                .findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(BlockNotFoundException::new);
    }

    @Override
    public void delete(Block target) {
        blockJpaRepository.delete(target);
    }

    @Override
    public boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return blockJpaRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public List<Long> findBlockedIdsByBlockerId(Long blockerId) {
        return blockJpaRepository.findBlockedIdsByBlockerId(blockerId);
    }

    @Override
    public List<Block> findAllByBlockerId(Long blockerId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(block)
                .where(block.blockerId.eq(blockerId))
                .orderBy(block.createdAt.desc(), block.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByBlockerId(Long blockerId) {
        return blockJpaRepository.countByBlockerId(blockerId);
    }
}
