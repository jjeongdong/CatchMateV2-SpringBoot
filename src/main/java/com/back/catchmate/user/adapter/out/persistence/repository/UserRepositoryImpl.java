package com.back.catchmate.user.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.UserRepository;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.back.catchmate.club.adapter.out.persistence.entity.QClubEntity.clubEntity;
import static com.back.catchmate.user.adapter.out.persistence.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.from(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return saved.toModel();
    }

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        return jpaUserRepository.findAllById(ids).stream()
                .map(UserEntity::toModel)
                .toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return jpaUserRepository.findByProviderId(providerId)
                .map(UserEntity::toModel);
    }

    @Override
    public List<User> findAllEventAlarmEnabled() {
        return jpaQueryFactory
                .selectFrom(userEntity)
                .join(userEntity.club, clubEntity).fetchJoin()
                .where(userEntity.eventAlarm.eq('Y'))
                .fetch()
                .stream()
                .map(UserEntity::toModel)
                .toList();
    }

    @Override
    public Page<User> findAllByClubName(String clubName, Pageable pageable) {
        List<UserEntity> entities = jpaQueryFactory
                .selectFrom(userEntity)
                .join(userEntity.club, clubEntity).fetchJoin()
                .where(clubNameEq(clubName))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(userEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(userEntity.count())
                .from(userEntity)
                .join(userEntity.club, clubEntity)
                .where(clubNameEq(clubName))
                .fetchOne();

        List<User> users = entities.stream()
                .map(UserEntity::toModel)
                .toList();

        return new PageImpl<>(users, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public Map<String, Long> countUsersByClub() {
        List<Tuple> results = jpaQueryFactory
                .select(clubEntity.name, userEntity.count())
                .from(userEntity)
                .join(userEntity.club, clubEntity)
                .groupBy(clubEntity.name)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(clubEntity.name),
                        tuple -> {
                            Long count = tuple.get(userEntity.count());
                            return count != null ? count : 0L;
                        }
                ));
    }

    @Override
    public Map<String, Long> countUsersByWatchStyle() {
        List<Tuple> results = jpaQueryFactory
                .select(userEntity.watchStyle, userEntity.count())
                .from(userEntity)
                .where(userEntity.watchStyle.isNotNull())
                .groupBy(userEntity.watchStyle)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(userEntity.watchStyle),
                        tuple -> {
                            Long count = tuple.get(userEntity.count());
                            return count != null ? count : 0L;
                        }
                ));
    }

    @Override
    public boolean existsByNickName(String nickName) {
        return jpaUserRepository.existsByNickName(nickName);
    }

    @Override
    public long count() {
        return jpaUserRepository.count();
    }

    @Override
    public long countByGender(Character gender) {
        return jpaUserRepository.countByGender(gender);
    }

    // 동적 쿼리 조건: clubName이 있으면 필터링, 없으면 전체 조회
    private BooleanExpression clubNameEq(String clubName) {
        if (clubName == null || clubName.isBlank()) {
            return null;
        }
        return clubEntity.name.eq(clubName);
    }
}
