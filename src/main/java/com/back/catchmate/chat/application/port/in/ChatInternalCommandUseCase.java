package com.back.catchmate.chat.application.port.in;

public interface ChatInternalCommandUseCase {
    /**
     * 게시글 ID에 해당하는 채팅방을 조회하거나 없으면 새로 생성하고 ID를 반환
     */
    Long getOrCreateChatRoom(Long boardId);

    void addMember(Long chatRoomId, Long userId);

    /**
     * 신규 입장 멤버에 대한 시스템 메시지를 생성하고 브로드캐스트 트리거 이벤트를 발행한다.
     * {@link com.back.catchmate.chat.application.event.ChatRoomMemberJoinedEvent} 리스너에서 호출.
     */
    void welcomeNewMember(Long chatRoomId, Long userId);

    /**
     * 게시글에 해당하는 채팅방을 보장하고 (없으면 생성) 멤버를 추가한 뒤 환영 시스템 메시지를 발행한다.
     * board.completed (작성자 입장) / enroll.accepted (신청자 입장) 양쪽에서 호출.
     */
    void addBoardChatRoomMember(Long boardId, Long userId);

    void flushReadSequences();

    void flushRoomSequences();
}
