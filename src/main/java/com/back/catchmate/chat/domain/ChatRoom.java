package com.back.catchmate.chat.domain;

import com.back.catchmate.chat.domain.exception.ChatRoomNotHostException;
import com.back.catchmate.chat.domain.exception.ChatSelfKickNotAllowedException;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_chat_rooms_board_id",
                    columnNames = {"board_id"})
        })
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    // 메시지마다 갱신하지 않고 Redis 버퍼를 모아 JDBC 배치로 올린다 (ChatRoomRepository.updateMaxSequences).
    @Column(name = "last_message_sequence", nullable = false)
    private Long lastMessageSequence;

    @Column(name = "chat_room_image_url")
    private String chatRoomImageUrl;

    private LocalDateTime deletedAt;

    private ChatRoom(Long boardId) {
        this.boardId = boardId;
        this.lastMessageSequence = 0L;
    }

    public static ChatRoom create(Long boardId) {
        return new ChatRoom(boardId);
    }

    public void changeImage(String imageUrl) {
        this.chatRoomImageUrl = imageUrl;
    }

    // 방장은 게시글 작성자다. 작성자 ID 는 board BC 에서 조회해 넘겨받는다.
    public void verifyKick(Long requesterId, Long boardWriterId, Long targetUserId) {
        if (!boardWriterId.equals(requesterId)) {
            throw new ChatRoomNotHostException();
        }
        if (requesterId.equals(targetUserId)) {
            throw new ChatSelfKickNotAllowedException();
        }
    }
}
