package com.back.catchmate.user.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.persistence.UserRepository;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return saved.toDomain();
    }

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        return jpaUserRepository.findAllById(ids).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return jpaUserRepository.findByProviderId(providerId)
                .map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAllEventAlarmEnabled() {
        return jpaQueryFactory
                .selectFrom(userEntity)
                .where(userEntity.eventAlarm.eq('Y'))
                .fetch()
                .stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public Page<User> findAllByClubId(Long clubId, Pageable pageable) {
        List<UserEntity> entities = jpaQueryFactory
                .selectFrom(userEntity)
                .where(clubId != null ? userEntity.clubId.eq(clubId) : null)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(userEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(userEntity.count())
                .from(userEntity)
                .where(clubId != null ? userEntity.clubId.eq(clubId) : null)
                .fetchOne();

        List<User> users = entities.stream()
                .map(UserEntity::toDomain)
                .toList();

        return new PageImpl<>(users, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public Map<Long, Long> countUsersGroupedByClubId() {
        List<Tuple> results = jpaQueryFactory
                .select(userEntity.clubId, userEntity.count())
                .from(userEntity)
                .groupBy(userEntity.clubId)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(userEntity.clubId),
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
}
