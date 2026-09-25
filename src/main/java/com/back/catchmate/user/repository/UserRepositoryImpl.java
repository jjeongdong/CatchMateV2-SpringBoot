package com.back.catchmate.user.repository;

import static com.back.catchmate.user.entity.QUser.user;

import com.back.catchmate.user.entity.User;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<User> findAllEventAlarmEnabled() {
        return jpaQueryFactory.selectFrom(user).where(user.eventAlarm.eq('Y')).fetch();
    }

    @Override
    public Page<User> findAllByClubId(Long clubId, Pageable pageable) {
        List<User> users = jpaQueryFactory
                .selectFrom(user)
                .where(clubId != null ? user.clubId.eq(clubId) : null)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(user.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(user.count())
                .from(user)
                .where(clubId != null ? user.clubId.eq(clubId) : null)
                .fetchOne();

        return new PageImpl<>(users, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public Map<Long, Long> countUsersGroupedByClubId() {
        List<Tuple> results = jpaQueryFactory
                .select(user.clubId, user.count())
                .from(user)
                .groupBy(user.clubId)
                .fetch();

        return results.stream().collect(Collectors.toMap(tuple -> tuple.get(user.clubId), tuple -> {
            Long count = tuple.get(user.count());
            return count != null ? count : 0L;
        }));
    }

    @Override
    public Map<String, Long> countUsersByWatchStyle() {
        List<Tuple> results = jpaQueryFactory
                .select(user.watchStyle, user.count())
                .from(user)
                .where(user.watchStyle.isNotNull())
                .groupBy(user.watchStyle)
                .fetch();

        return results.stream().collect(Collectors.toMap(tuple -> tuple.get(user.watchStyle), tuple -> {
            Long count = tuple.get(user.count());
            return count != null ? count : 0L;
        }));
    }
}
