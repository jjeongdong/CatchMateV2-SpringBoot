package com.back.catchmate.user.infrastructure;

import static com.back.catchmate.user.domain.QUser.user;

import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.exception.UserNotFoundException;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public User getById(Long userId) {
        return userJpaRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public List<User> findAllByIds(Collection<Long> userIds) {
        return userJpaRepository.findAllById(userIds);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return userJpaRepository.findByProviderId(providerId);
    }

    @Override
    public boolean existsByNickName(String nickName) {
        return userJpaRepository.existsByNickName(nickName);
    }

    @Override
    public List<User> findAllEventAlarmEnabled() {
        return jpaQueryFactory.selectFrom(user).where(user.eventAlarm.eq('Y')).fetch();
    }

    @Override
    public List<User> findAllByClubId(Long clubId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(user)
                .where(clubIdEq(clubId))
                .orderBy(user.createdAt.desc(), user.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByClubId(Long clubId) {
        Long count = jpaQueryFactory
                .select(user.count())
                .from(user)
                .where(clubIdEq(clubId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public Map<Long, Long> countGroupedByClubId() {
        List<Tuple> rows = jpaQueryFactory
                .select(user.clubId, user.count())
                .from(user)
                .groupBy(user.clubId)
                .fetch();
        return rows.stream().collect(Collectors.toMap(row -> row.get(user.clubId), row -> countOf(row)));
    }

    @Override
    public Map<String, Long> countGroupedByWatchStyle() {
        List<Tuple> rows = jpaQueryFactory
                .select(user.watchStyle, user.count())
                .from(user)
                .where(user.watchStyle.isNotNull())
                .groupBy(user.watchStyle)
                .fetch();
        return rows.stream().collect(Collectors.toMap(row -> row.get(user.watchStyle), row -> countOf(row)));
    }

    @Override
    public long count() {
        return userJpaRepository.count();
    }

    @Override
    public long countByGender(Character gender) {
        return userJpaRepository.countByGender(gender);
    }

    // QueryDSL 은 where 인자가 null 이면 그 조건을 건너뛰므로, 필터가 없을 때 null 을 돌려준다.
    private BooleanExpression clubIdEq(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return user.clubId.eq(clubId);
    }

    private long countOf(Tuple row) {
        Long count = row.get(user.count());
        return count != null ? count : 0L;
    }
}
