package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.application.dto.command.UserAlarmUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserCreateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserCreateResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.ProfileImageUploader;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserAlarmType;
import com.back.catchmate.user.domain.UserPresenceRepository;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.exception.UserAlreadyExistsException;
import com.back.catchmate.user.fixture.UserFixture;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPresenceRepository userPresenceRepository;

    @Mock
    private ProfileImageUploader profileImageUploader;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private UserCommandService userCommandService;

    @Nested
    @DisplayName("내 프로필 수정")
    class UpdateMyProfile {

        @Test
        @DisplayName("이미지가 있으면 업로드한 URL 로 바꾸고 구단 정보를 담아 돌려준다")
        void uploadsImageAndReturnsResult() {
            // given
            User user = UserFixture.user(1L, 3L);
            UploadFile image = new UploadFile("a.png", "image/png", new ByteArrayInputStream(new byte[] {1}), 1);
            given(userRepository.getById(1L)).willReturn(user);
            given(profileImageUploader.upload(image)).willReturn("https://cdn/a.png");
            given(clubQueryApi.getInfo(5L)).willReturn(new ClubInfo(5L, "LG 트윈스", "잠실", "서울"));

            // when
            UserResult result =
                    userCommandService.updateMyProfile(1L, new UserProfileUpdateCommand("새닉네임", 5L, null), image);

            // then
            assertThat(result.nickName()).isEqualTo("새닉네임");
            assertThat(result.profileImageUrl()).isEqualTo("https://cdn/a.png");
            assertThat(result.club().name()).isEqualTo("LG 트윈스");
        }

        @Test
        @DisplayName("이미지가 없으면 업로드하지 않고 기존 이미지를 유지한다")
        void keepsImageWhenNoUpload() {
            // given
            User user = UserFixture.user(1L, 3L);
            String originalImage = user.getProfileImageUrl();
            given(userRepository.getById(1L)).willReturn(user);
            given(clubQueryApi.getInfo(3L)).willReturn(new ClubInfo(3L, "두산 베어스", "잠실", "서울"));

            // when
            UserResult result =
                    userCommandService.updateMyProfile(1L, new UserProfileUpdateCommand(null, null, "직관형"), null);

            // then
            assertThat(result.profileImageUrl()).isEqualTo(originalImage);
            assertThat(result.watchStyle()).isEqualTo("직관형");
            then(profileImageUploader).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("알림을 바꾸면 바뀐 뒤의 전체 알림 상태를 돌려준다")
    void updateMyAlarmReturnsAllStates() {
        // given
        given(userRepository.getById(1L)).willReturn(UserFixture.user(1L, 3L));

        // when
        UserAlarmResult result =
                userCommandService.updateMyAlarm(1L, new UserAlarmUpdateCommand(UserAlarmType.CHAT, false));

        // then
        assertThat(result).isEqualTo(new UserAlarmResult(true, false, true, true));
    }

    @Nested
    @DisplayName("가입")
    class CreateUser {

        private final UserCreateCommand command = new UserCreateCommand(
                "KAKAO", "KAKAO_1", "a@catchmate.com", "홍길동", 'M', LocalDate.of(2000, 1, 1), 3L, "url", null);

        @Test
        @DisplayName("같은 제공자 식별자로 가입한 유저가 있으면 UserAlreadyExistsException")
        void rejectsDuplicate() {
            // given
            given(userRepository.findByProviderId("KAKAO_1")).willReturn(Optional.of(UserFixture.user(1L, 3L)));

            // when & then
            assertThatThrownBy(() -> userCommandService.createUser(command))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        @DisplayName("저장한 유저의 ID 와 권한을 돌려준다")
        void createsUser() {
            // given
            given(userRepository.findByProviderId("KAKAO_1")).willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(UserFixture.user(7L, 3L));

            // when
            UserCreateResult result = userCommandService.createUser(command);

            // then
            assertThat(result.userId()).isEqualTo(7L);
            assertThat(result.authority()).isEqualTo("ROLE_USER");
        }
    }

    @Test
    @DisplayName("신고 처리하면 유저가 reported 상태가 된다")
    void markUserAsReported() {
        // given
        User user = UserFixture.user(1L, 3L);
        given(userRepository.getById(1L)).willReturn(user);

        // when
        userCommandService.markUserAsReported(1L);

        // then
        assertThat(user.isReported()).isTrue();
    }

    @Test
    @DisplayName("온라인 상태 쓰기는 저장소에 위임한다")
    void delegatesPresence() {
        // when
        userCommandService.markOnline(1L);
        userCommandService.focusRoom(1L, 11L);
        userCommandService.unfocusRoom(1L);
        userCommandService.markOffline(1L);

        // then
        then(userPresenceRepository).should().markOnline(1L);
        then(userPresenceRepository).should().focusRoom(1L, 11L);
        then(userPresenceRepository).should().unfocusRoom(1L);
        then(userPresenceRepository).should().markOffline(1L);
    }
}
