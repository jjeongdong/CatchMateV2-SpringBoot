package com.back.catchmate.infrastructure.persistence.user.repository;

import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.repository.UserRepository;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

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
}
