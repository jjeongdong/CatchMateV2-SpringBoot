package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.chat.domain.ChatMembershipCache;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.fixture.ChatFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatMembershipReaderTest {

    @Mock
    private ChatMembershipCache chatMembershipCache;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatMembershipReader chatMembershipReader;

    @Test
    @DisplayName("캐시에 있으면 DB 를 읽지 않는다")
    void cacheHit() {
        given(chatMembershipCache.find(5L, 1L)).willReturn(Optional.of(new MembershipSnapshot(true, false)));

        assertThat(chatMembershipReader.get(5L, 1L)).isEqualTo(new MembershipSnapshot(true, false));
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("캐시에 없으면 DB 에서 읽어 캐시에 넣는다")
    void cacheMissLoads() {
        given(chatMembershipCache.find(5L, 1L)).willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(5L, 1L))
                .willReturn(Optional.of(ChatFixture.member(1L, ChatFixture.room(5L, 10L, 0), 1L, 0)));

        MembershipSnapshot snapshot = chatMembershipReader.get(5L, 1L);

        assertThat(snapshot).isEqualTo(new MembershipSnapshot(true, false));
        then(chatMembershipCache).should().put(5L, 1L, snapshot);
    }

    @Test
    @DisplayName("멤버 행이 없으면 ChatRoomMemberNotFoundException 이고 캐시에 넣지 않는다")
    void noMember() {
        given(chatMembershipCache.find(5L, 1L)).willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(5L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatMembershipReader.get(5L, 1L)).isInstanceOf(ChatRoomMemberNotFoundException.class);
        then(chatMembershipCache).should(never()).put(anyLong(), anyLong(), any());
    }
}
