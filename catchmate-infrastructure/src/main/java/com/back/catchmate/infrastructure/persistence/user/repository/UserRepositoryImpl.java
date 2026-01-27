package com.back.catchmate.infrastructure.persistence.user.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.repository.UserRepository;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.back.catchmate.infrastructure.persistence.club.entity.QClubEntity.clubEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

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
    public Optional<User> findByProviderId(String providerId) {
        return jpaUserRepository.findByProviderId(providerId)
                .map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toModel);
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
    public DomainPage<User> findAllByClubName(String clubName, DomainPageable pageable) {
        List<UserEntity> entities = jpaQueryFactory
                .selectFrom(userEntity)
                .join(userEntity.club, clubEntity).fetchJoin()
                .where(clubNameEq(clubName))
                .offset(pageable.getOffset())
                .limit(pageable.getSize())
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

        return new DomainPage<>(
                users,
                pageable.getPage(),
                pageable.getSize(),
                totalCount != null ? totalCount : 0L
        );
    }

    // 동적 쿼리 조건: clubName이 있으면 필터링, 없으면 전체 조회
    private BooleanExpression clubNameEq(String clubName) {
        if (clubName == null || clubName.isBlank()) {
            return null;
        }
        return clubEntity.name.eq(clubName);
    }
}
