package com.back.catchmate.user.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.application.dto.command.CreateUserCommand;
import com.back.catchmate.user.application.dto.response.CreatedUserResponse;
import com.back.catchmate.user.application.port.in.UserInternalCommandUseCase;
import com.back.catchmate.user.application.port.out.persistence.UserRepository;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserInternalCommandService implements UserInternalCommandUseCase {
    private final UserRepository userRepository;
    private final UserReader userReader;

    @Override
    public CreatedUserResponse createUser(CreateUserCommand command) {
        if (userRepository.findByProviderId(command.providerIdWithProvider()).isPresent()) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.createUser(
                command.provider(),
                command.providerIdWithProvider(),
                command.email(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                command.profileImageUrl(),
                null,
                command.watchStyle()
        );
        User savedUser = userRepository.save(user);
        return CreatedUserResponse.from(savedUser);
    }

    @Override
    public void markUserAsReported(Long userId) {
        User user = userReader.getUser(userId);
        user.markAsReported();
        userRepository.save(user);
    }

    @Override
    public void clearFcmToken(Long userId) {
        User user = userReader.getUser(userId);
        user.deleteFcmToken();
        userRepository.save(user);
    }
}
