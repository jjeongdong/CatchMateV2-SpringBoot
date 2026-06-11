# Catchmate 채팅 시스템 아키텍처

## 목차

1. [시스템 개요](#1-시스템-개요)
2. [핵심 구성 요소](#2-핵심-구성-요소)
3. [메시지 전송 흐름](#3-메시지-전송-흐름)
4. [메시지 수신 흐름](#4-메시지-수신-흐름)
5. [읽음 처리 시스템](#5-읽음-처리-시스템)
6. [채팅 히스토리 조회 및 캐싱](#6-채팅-히스토리-조회-및-캐싱)
7. [채팅방 관리](#7-채팅방-관리)
8. [알림 시스템](#8-알림-시스템)
9. [멀티 인스턴스 구조](#9-멀티-인스턴스-구조)
10. [데이터 모델](#10-데이터-모델)
11. [API 명세](#11-api-명세)

---

## 1. 시스템 개요

Catchmate 채팅 시스템은 **WebSocket(STOMP) + Redis Pub/Sub** 기반의 실시간 채팅 시스템입니다.
멀티 서버 인스턴스 환경을 지원하며, 읽음 상태 버퍼링과 히스토리 캐싱으로 성능을 최적화합니다.

### 기술 스택

| 기술 | 역할 |
|---|---|
| WebSocket + STOMP | 클라이언트-서버 실시간 양방향 통신 |
| Redis Pub/Sub | 서버 인스턴스 간 메시지 브로드캐스트 |
| Redis Hash + Lua Script | 읽음 시퀀스 버퍼링 (원자적 연산) |
| Redis INCR | 메시지 시퀀스 번호 생성 |
| Redis Cache | 채팅 히스토리 캐싱 (1시간 TTL) |
| MySQL (JPA + QueryDSL) | 영구 데이터 저장 |
| FCM (Firebase) | 오프라인 사용자 푸시 알림 |

### 전체 아키텍처 다이어그램

```
+------------------+          +------------------+
|   Mobile Client  |          |   Mobile Client  |
|   (WebSocket)    |          |   (WebSocket)    |
+--------+---------+          +--------+---------+
         |                             |
         |  STOMP /pub/chat/message    |  STOMP /sub/chat/room/{id}
         v                             v
+--------+-----------------------------+---------+
|              Nginx (Load Balancer)              |
+-----------+-----------------------+-------------+
            |                       |
            v                       v
   +--------+--------+    +--------+--------+
   |  Server Inst. A  |    |  Server Inst. B  |
   |                  |    |                  |
   |  +-----------+   |    |  +-----------+   |
   |  | WebSocket |   |    |  | WebSocket |   |
   |  | (STOMP)   |   |    |  | (STOMP)   |   |
   |  +-----+-----+   |    |  +-----+-----+   |
   |        |          |    |        |          |
   |  +-----+-----+   |    |  +-----+-----+   |
   |  |Orchestrator|   |    |  |Orchestrator|   |
   |  +-----+-----+   |    |  +-----+-----+   |
   |        |          |    |        |          |
   +--------+----------+    +--------+----------+
            |                        |
            v                        v
   +--------+------------------------+----------+
   |                  Redis                      |
   |                                             |
   |  +-------------+  +---------+  +--------+  |
   |  | Pub/Sub     |  | Cache   |  | Seq    |  |
   |  | (broadcast) |  | (hist.) |  | (INCR) |  |
   |  +-------------+  +---------+  +--------+  |
   |                                             |
   |  +------------------------------------------+
   |  | Hash: chat:read-sequence:buffer          |
   |  +------------------------------------------+
   +---------------------------------------------+
            |
            v
   +--------+--------+
   |     MySQL       |
   |  +-----------+  |
   |  | messages  |  |
   |  | rooms     |  |
   |  | members   |  |
   |  +-----------+  |
   +-----------------+
```

---

## 2. 핵심 구성 요소

### 모듈별 채팅 관련 클래스

```
catchmate-api
 +-- ChatController              (WebSocket STOMP 엔드포인트)
 +-- ChatRestController          (REST API 엔드포인트)
 +-- WebSocketEventListener      (연결/구독 이벤트 처리)
 +-- WebSocketConfig             (STOMP 설정)
 +-- ReadSequenceFlushScheduler  (읽음 상태 배치 플러시)

catchmate-orchestration
 +-- ChatOrchestrator            (트랜잭션 경계, 서비스 조율)

catchmate-application
 +-- ChatService                 (메시지 저장, 히스토리 조회)
 +-- ChatRoomService             (채팅방 생성/조회)
 +-- ChatRoomMemberService       (멤버 입장/퇴장/강퇴)
 +-- ChatNotificationEventListener (알림 이벤트 처리)

catchmate-domain
 +-- ChatMessage, ChatRoom, ChatRoomMember    (도메인 모델)
 +-- ChatSequencePort            (시퀀스 생성 포트)
 +-- ChatHistoryCachePort        (캐시 무효화 포트)
 +-- ReadSequenceBufferPort      (읽음 버퍼 포트)

catchmate-infrastructure
 +-- ChatSequenceAdapter         (Redis INCR 구현)
 +-- RedisChatHistoryCacheAdapter (캐시 무효화 구현)
 +-- RedisReadSequenceBufferAdapter (Lua 스크립트 기반 버퍼)
 +-- RedisPublisher / RedisSubscriber (Pub/Sub)
 +-- QueryDSLChatMessageRepository  (히스토리 쿼리)
```

---

## 3. 메시지 전송 흐름

사용자가 메시지를 보내면 다음 과정을 거칩니다.

### 흐름 다이어그램

```
  Client (WebSocket)
      |
      | STOMP SEND /pub/chat/message
      | { chatRoomId, content, messageType }
      |
      v
  +---+-------------------+
  |   ChatController       |
  |   .sendMessage()       |
  |   - JWT에서 userId 추출 |
  +---+-------------------+
      |
      v
  +---+-------------------+
  |   ChatOrchestrator     |
  |   .sendMessage()       |
  |   - User 조회          |
  +---+-------------------+
      |
      v
  +---+-----------------------------+
  |   ChatService.saveMessage()      |  <-- @Transactional
  |                                  |
  |   1. ChatRoom 조회               |
  |   2. Redis INCR -> 시퀀스 생성    |
  |      chat:room:{roomId}:seq      |
  |   3. ChatRoom.lastMessageSequence|
  |      업데이트                     |
  |   4. ChatMessage 생성 (시퀀스 포함)|
  |   5. 발신자 읽음 버퍼에 기록       |
  |      Redis Hash (Lua Script)     |
  |   6. DB에 메시지 저장 (JPA)       |
  |   7. 최신 페이지 캐시 무효화       |
  |      chatHistory::{roomId}_START_*|
  +---+-----------------------------+
      |
      | 트랜잭션 커밋
      |
      v
  +---+-------------------------------+
  |   이벤트 발행 (트랜잭션 커밋 후)     |
  |                                    |
  |   +-- ChatMessageEvent             |
  |   |   -> RedisPublisher            |
  |   |   -> Redis Pub/Sub 브로드캐스트  |
  |   |                                |
  |   +-- ChatNotificationEvent        |
  |       -> FCM 알림 처리              |
  +------------------------------------+
```

### 시퀀스 번호 생성 상세

```
  ChatSequenceAdapter
      |
      | Redis: INCR chat:room:{roomId}:seq
      |
      | 예시: Room 42의 메시지 흐름
      |   INCR -> 1 (첫 번째 메시지)
      |   INCR -> 2 (두 번째 메시지)
      |   INCR -> 3 (세 번째 메시지)
      |
      v
  원자적으로 증가하는 고유 번호 반환
  -> 동시 요청에도 중복 없음
```

---

## 4. 메시지 수신 흐름

메시지가 저장된 후, 모든 서버 인스턴스의 클라이언트에게 전달됩니다.

### 흐름 다이어그램

```
  +---------------------------+
  | RedisPublisher            |
  | @TransactionalEventListener|
  | (AFTER_COMMIT, @Async)    |
  +------------+--------------+
               |
               | publish(ChatMessageEvent)
               v
  +------------+--------------+
  |       Redis Pub/Sub       |
  |  Topic: catchmate-chat-   |
  |         topic             |
  +--+------------------+-----+
     |                  |
     v                  v
  +--+-------+    +----+------+
  | Server A |    | Server B  |
  | Subscriber|   | Subscriber|
  +--+-------+    +----+------+
     |                  |
     | onMessage()      | onMessage()
     | JSON 역직렬화     | JSON 역직렬화
     v                  v
  +--+-------+    +----+------+
  | STOMP    |    | STOMP     |
  | Broker   |    | Broker    |
  +--+-------+    +----+------+
     |                  |
     | /sub/chat/       | /sub/chat/
     | room/{roomId}    | room/{roomId}
     v                  v
  +--+-------+    +----+------+
  | Client 1 |    | Client 2  |
  | Client 3 |    | Client 4  |
  +----------+    +-----------+

  * 각 서버는 자신에게 연결된 클라이언트에게만 전달
  * Redis Pub/Sub이 서버 간 동기화를 담당
```

### WebSocket 구독 구조

```
  클라이언트 WebSocket 연결 및 구독 흐름:

  1. WebSocket 연결
     ws://server/ws/chat
        |
        v
     StompAuthChannelInterceptor
        - JWT 토큰 검증
        - Principal 설정

  2. 채팅방 구독
     SUBSCRIBE /sub/chat/room/{roomId}
        |
        v
     WebSocketEventListener.handleSessionSubscribeEvent()
        |
        +-- chatOrchestrator.readChatRoom(userId, roomId)
        |   (현재 시점까지 읽음 처리)
        |
        +-- userOnlineStatusOrchestrator.setUserFocusRoom(userId, roomId)
            (현재 보고 있는 방 기록 -> 알림 필터링에 활용)

  3. 메시지 수신
     MESSAGE /sub/chat/room/{roomId}
     { messageId, senderId, content, ... }
```

---

## 5. 읽음 처리 시스템

읽음 상태는 빈번한 DB 업데이트를 방지하기 위해 **Redis 버퍼 + 배치 플러시** 전략을 사용합니다.

### 전체 흐름 다이어그램

```
  +------------------+     +------------------+     +------------------+
  |  읽음 이벤트 발생  |     |  Redis 버퍼 저장   |     |  5초마다 DB 플러시 |
  +------------------+     +------------------+     +------------------+

  [트리거 1]                       [버퍼]                    [플러시]
  WebSocket 구독 시          Redis Hash               ReadSequenceFlush
  (/sub/chat/room/{id})     chat:read-sequence:       Scheduler
       |                    buffer                    (5초 간격)
       |                         |                         |
       v                         |                         v
  readChatRoom()                 |                    drainAll()
       |                         |                    +-- HGETALL
       +------------------------>|                    +-- DEL
                                 |                    (Lua 원자적)
  [트리거 2]                      |                         |
  메시지 전송 시                   |                         v
  (발신자 자동 읽음)               |                    각 항목마다
       |                         |                    DB UPDATE
       +------------------------>|                    chat_room_members
                                 |                    .last_read_sequence
  [트리거 3]                      |
  REST 히스토리 조회 시            |
       |                         |
       +------------------------>|
```

### Redis 버퍼 상세 동작 (Lua Script)

```
  buffer(chatRoomId=42, userId=7, sequence=150)
      |
      v
  +---+--------------------------------------+
  | Lua Script (원자적 실행)                   |
  |                                           |
  |   Key: "chat:read-sequence:buffer"        |
  |   Field: "42:7"                           |
  |                                           |
  |   current = HGET(key, field)              |
  |   if current == nil or current < 150:     |
  |       HSET(key, field, 150)               |
  |                                           |
  |   * 항상 최대값만 유지                      |
  |   * 이전 시퀀스로 덮어쓰기 방지              |
  +-------------------------------------------+

  Redis Hash 상태 예시:
  +------------------------------+
  | chat:read-sequence:buffer    |
  |------------------------------|
  | "42:7"   ->  150             |  Room 42, User 7: seq 150까지 읽음
  | "42:12"  ->  148             |  Room 42, User 12: seq 148까지 읽음
  | "15:7"   ->  89              |  Room 15, User 7: seq 89까지 읽음
  +------------------------------+
```

### 플러시 프로세스

```
  ReadSequenceFlushScheduler (매 5초)
      |
      v
  drainAll() -- Lua Script
      |
      | 1. HGETALL -> 모든 항목 읽기
      | 2. DEL -> 버퍼 초기화
      | (하나의 원자적 연산)
      |
      v
  반환: { "42:7": 150, "42:12": 148, "15:7": 89 }
      |
      | 각 항목 파싱
      v
  +---+-------------------------------------------+
  | chatRoomMemberRepository                       |
  |   .updateLastReadSequenceDirectly(             |
  |       chatRoomId=42, userId=7, sequence=150)   |
  |                                                |
  |   UPDATE chat_room_members                     |
  |   SET last_read_sequence = 150                 |
  |   WHERE chat_room_id = 42                      |
  |     AND user_id = 7                            |
  |     AND last_read_sequence < 150  <-- 역전 방지  |
  +------------------------------------------------+
```

### 안 읽은 메시지 수 계산

```
  ChatRoom.lastMessageSequence = 200   (방의 마지막 메시지 번호)
  ChatRoomMember.lastReadSequence = 185 (내가 마지막으로 읽은 번호)

  안 읽은 메시지 수 = 200 - 185 = 15개
```

---

## 6. 채팅 히스토리 조회 및 캐싱

### 조회 흐름

```
  Client
      |
      | GET /api/chat/rooms/{roomId}/messages
      |     ?lastMessageId=500&size=20
      v
  +---+-------------------+
  | ChatRestController     |
  |  1. readChatRoom()     |  <-- 읽음 처리
  |  2. getChatHistory()   |
  +---+-------------------+
      |
      v
  +---+-------------------------------------------+
  | @Cacheable("chatHistory")                      |
  | key = "{roomId}_{lastMessageId}_{size}"        |
  |                                                |
  |  캐시에 있으면 -> 즉시 반환                       |
  |  캐시에 없으면 -> DB 조회 후 캐시 저장             |
  +---+-------------------------------------------+
      |
      | 캐시 미스 시
      v
  +---+-------------------------------------------+
  | QueryDSLChatMessageRepository                  |
  |   .findChatHistory(roomId, lastMessageId, 20)  |
  |                                                |
  |   Step 1: ID만 조회 (가벼운 쿼리)                |
  |   SELECT id FROM chat_messages                 |
  |   WHERE chat_room_id = {roomId}                |
  |     AND id < {lastMessageId}                   |
  |   ORDER BY id DESC                             |
  |   LIMIT 20                                     |
  |                                                |
  |   Step 2: 엔티티 + 발신자 조인                   |
  |   SELECT m.*, u.*                              |
  |   FROM chat_messages m                         |
  |   JOIN users u ON m.sender_id = u.id           |
  |   WHERE m.id IN (...)                          |
  |   ORDER BY m.id ASC                            |
  +------------------------------------------------+
```

### 캐시 전략 다이어그램

```
  캐시 키 구조:
  chatHistory::{roomId}_{lastMessageId}_{size}

  예시 (Room 42, 페이지 크기 20):
  +-----------------------------------------------+
  | chatHistory::42_START_20   <- 최신 페이지        |
  | chatHistory::42_500_20     <- 이전 페이지 1      |
  | chatHistory::42_480_20     <- 이전 페이지 2      |
  +-----------------------------------------------+


  새 메시지 저장 시 캐시 무효화:
  +-----------------------------------------------+
  | evictLatestPage(roomId=42)                     |
  |                                                |
  | Redis SCAN: chatHistory::42_START_*            |
  |   -> 매칭되는 키 삭제                             |
  |                                                |
  | chatHistory::42_START_20   [삭제됨]              |
  | chatHistory::42_500_20     [유지] <- 불변 데이터  |
  | chatHistory::42_480_20     [유지] <- 불변 데이터  |
  +-----------------------------------------------+

  * 최신 페이지만 무효화 (새 메시지가 추가되므로)
  * 과거 페이지는 변하지 않으므로 캐시 유지
  * TTL 1시간 후 자동 만료
```

### Sync API (재접속 시 누락 메시지 보완)

```
  WebSocket 재접속 시:

  Client                          Server
    |                               |
    | GET /sync?lastMessageId=180   |
    |------------------------------>|
    |                               |
    |   SELECT * FROM chat_messages |
    |   WHERE id > 180              |
    |   ORDER BY id ASC             |
    |                               |
    |  [msg 181, 182, ..., 195]     |
    |<------------------------------|
    |                               |
    | 누락된 메시지 UI에 반영          |

  * 캐싱하지 않음 (실시간 데이터)
  * WebSocket 끊김 사이의 메시지를 보완
```

---

## 7. 채팅방 관리

### 채팅방 생명주기

```
  +--------+     +----------+     +---------+     +--------+
  | Board  | --> | ChatRoom | --> | Members | --> | 메시지  |
  | 생성    |     | 자동 생성  |     | 입장     |     | 교환   |
  +--------+     +----------+     +---------+     +--------+
                                       |
                                       v
                              +-----------------+
                              |  퇴장 / 강퇴     |
                              |  leftAt 설정     |
                              +-----------------+
```

### 입장/퇴장/강퇴 처리

```
  [입장]
  addMember(chatRoom, user)
      |
      +-- 중복 활성 멤버 확인
      +-- ChatRoomMember 생성
      |   lastReadSequence = room.lastMessageSequence
      |   (입장 이전 메시지는 읽음 처리)
      +-- SYSTEM 메시지: "{user} 님이 입장하셨습니다."


  [퇴장]
  removeMember(chatRoom, user)
      |
      +-- leftAt = now() (소프트 삭제)
      +-- SYSTEM 메시지: "{user} 님이 퇴장하셨습니다."


  [강퇴] (방장만 가능)
  kickMember(chatRoom, targetUser)
      |
      +-- 요청자가 방장인지 검증
      +-- leftAt = now()
      +-- SYSTEM 메시지: "{user} 님이 내보내졌습니다."
      +-- @CacheEvict("chatRoomMemberAuth")
          (강퇴된 사용자의 권한 캐시 즉시 무효화)


  [활성 멤버 판별]
  leftAt IS NULL -> 활성 멤버
  leftAt IS NOT NULL -> 퇴장/강퇴된 멤버
```

---

## 8. 알림 시스템

### Transactional Outbox 패턴

```
  ChatOrchestrator.sendMessage()
      |
      | publishEvent(ChatNotificationEvent)
      |
      v
  +---+-------------------------------------------------+
  |          Phase 1: @EventListener (트랜잭션 내)         |
  |                                                      |
  |   ChatNotificationEventListener.saveNotification()   |
  |       |                                              |
  |       +-- Notification 엔티티 저장 (PENDING)           |
  |       +-- NotificationOutbox 엔티티 저장 (PENDING)     |
  |                                                      |
  |   * 메시지 저장과 같은 트랜잭션에서 실행                   |
  |   * 메시지가 저장되면 알림도 반드시 저장됨                  |
  +------------------------------------------------------+
      |
      | 트랜잭션 커밋
      |
      v
  +---+-------------------------------------------------+
  |   Phase 2: @TransactionalEventListener (커밋 후, Async)|
  |                                                      |
  |   handleChatNotification()                           |
  |       |                                              |
  |       +-- 수신자 온라인 상태 확인 (Redis)               |
  |       |                                              |
  |       +-- [온라인 + 같은 방 보는 중]                    |
  |       |   -> 알림 스킵 (이미 메시지를 보고 있음)          |
  |       |                                              |
  |       +-- [온라인 + 다른 방 보는 중]                    |
  |       |   -> WebSocket 알림 전송                       |
  |       |                                              |
  |       +-- [오프라인]                                   |
  |           -> FCM 푸시 발송 시도                         |
  |           -> 성공: Outbox -> SUCCESS                   |
  |           -> 실패: retry 카운트 증가                    |
  +------------------------------------------------------+
      |
      | 실패한 알림은 스케줄러가 재시도
      v
  +---+-------------------------------------------------+
  |   NotificationScheduler (60초마다)                    |
  |                                                      |
  |   1. PENDING/FAILED 상태 Outbox 조회                  |
  |   2. FCM 발송 재시도                                   |
  |   3. 성공 -> SUCCESS                                  |
  |   4. 실패 -> retry 카운트 증가                          |
  |   5. max retry 초과 -> FAILED (최종 실패)               |
  +------------------------------------------------------+
```

### 알림 필터링 로직

```
  수신자 상태별 알림 분기:

                    수신자 상태 확인
                         |
            +------------+------------+
            |            |            |
         오프라인      온라인         온라인
                    (다른 방)      (같은 방)
            |            |            |
            v            v            v
        FCM Push    WebSocket      스킵
        (Outbox)    알림 전송     (이미 보는 중)

  * "같은 방" 판별: Redis에 저장된 focusRoom과 비교
  * focusRoom은 WebSocket 구독/해제 시 설정/해제
```

---

## 9. 멀티 인스턴스 구조

### Redis Pub/Sub 기반 메시지 동기화

```
  +------------------+                    +------------------+
  |  Server A        |                    |  Server B        |
  |                  |                    |                  |
  |  Client 1 (R:42) |                    |  Client 3 (R:42) |
  |  Client 2 (R:15) |                    |  Client 4 (R:15) |
  +--------+---------+                    +--------+---------+
           |                                       |
           |         +-----------------+           |
           +-------->|   Redis Topic   |<----------+
           |         | catchmate-chat- |           |
           |         | topic           |           |
           |         +--------+--------+           |
           |                  |                    |
           |    메시지 발행     |    구독 수신        |
           |    (Server A)    |    (모든 서버)      |
           |                  |                    |
           v                  v                    v
  +--------+---------+  +----+-----+  +---------+--------+
  | 발행 서버도       |  |          |  | 수신 서버도       |
  | 동일하게 수신     |  |  Redis   |  | 동일하게 수신     |
  +------------------+  +----------+  +------------------+

  Room 42에 메시지가 오면:
  - Server A: Client 1에게 전달 (Room 42 구독 중)
  - Server A: Client 2에게 전달 안 함 (Room 15 구독 중)
  - Server B: Client 3에게 전달 (Room 42 구독 중)
  - Server B: Client 4에게 전달 안 함 (Room 15 구독 중)
```

### WebSocket 재접속 보완 전략

```
  정상 상태:       WebSocket으로 실시간 수신
                         |
  연결 끊김:        메시지 유실 가능
                         |
  재접속:           1. WebSocket 재연결
                   2. SUBSCRIBE /sub/chat/room/{roomId}
                         |
                   3. GET /api/chat/rooms/{roomId}/sync
                      ?lastMessageId={마지막 수신 ID}
                         |
                   4. 누락 메시지 수신 및 UI 반영
                         |
  정상 상태:       WebSocket으로 실시간 수신 재개
```

---

## 10. 데이터 모델

### ERD

```
  +-------------------+       +---------------------+       +------------------------+
  |    chat_rooms     |       |   chat_room_members  |       |    chat_messages        |
  +-------------------+       +---------------------+       +------------------------+
  | id           (PK) |<--+   | id             (PK) |       | id                (PK) |
  | board_id     (FK) |   +---| chat_room_id   (FK) |   +-->| chat_room_id      (FK) |
  | last_message_seq  |       | user_id        (FK) |   |   | sender_id         (FK) |
  | image_url         |       | last_read_seq       |   |   | sequence               |
  | deleted_at        |       | is_notification_on   |   |   | content                |
  +-------------------+       | joined_at           |   |   | message_type           |
         |                    | left_at             |   |   | deleted_at             |
         |                    +---------------------+   |   +------------------------+
         |                                              |
         +----------------------------------------------+


  시퀀스 기반 읽음 추적:

  chat_rooms.last_message_sequence = 200
       |
       |  Room 42의 메시지 시퀀스: 1, 2, 3, ..., 198, 199, 200
       |
  chat_room_members (Room 42):
       |
       +-- User A: last_read_sequence = 200  (안 읽은 수: 0)
       +-- User B: last_read_sequence = 185  (안 읽은 수: 15)
       +-- User C: last_read_sequence = 150  (안 읽은 수: 50)
```

### Redis 키 구조

```
  +-------------------------------------------+---------------------------+
  | Key                                       | 용도                       |
  +-------------------------------------------+---------------------------+
  | chat:room:{roomId}:seq                    | 메시지 시퀀스 카운터 (INCR)  |
  | chat:read-sequence:buffer                 | 읽음 상태 버퍼 (Hash)        |
  | chatHistory::{roomId}_{msgId}_{size}      | 히스토리 캐시              |
  | chatRoomMemberAuth::{roomId}_{userId}     | 멤버 권한 캐시 (1시간)       |
  | catchmate-chat-topic                      | Pub/Sub 토픽              |
  | user:online:{userId}                      | 온라인 상태                 |
  | user:focus-room:{userId}                  | 현재 보고 있는 채팅방         |
  +-------------------------------------------+---------------------------+
```

---

## 11. API 명세

### WebSocket (STOMP) 엔드포인트

| 방향 | Destination | 설명 |
|---|---|---|
| Client -> Server | `/pub/chat/message` | 메시지 전송 |
| Client -> Server | `/pub/chat/enter` | 채팅방 입장 |
| Client -> Server | `/pub/chat/leave` | 채팅방 퇴장 |
| Client -> Server | `/pub/chat/read` | 읽음 처리 |
| Server -> Client | `/sub/chat/room/{roomId}` | 실시간 메시지 수신 |

### REST API 엔드포인트

| Method | URL | 설명 |
|---|---|---|
| `GET` | `/api/chat/rooms` | 채팅방 목록 (페이징, 안읽은 수 포함) |
| `GET` | `/api/chat/rooms/{roomId}/messages` | 채팅 히스토리 (커서 기반 페이징) |
| `GET` | `/api/chat/rooms/{roomId}/sync` | 누락 메시지 동기화 |
| `GET` | `/api/chat/rooms/{roomId}/messages/last` | 마지막 메시지 조회 |
| `GET` | `/api/chat/rooms/{roomId}/members` | 채팅방 멤버 목록 |
| `PUT` | `/api/chat/rooms/{roomId}/notifications` | 알림 ON/OFF 토글 |
| `PATCH` | `/api/chat/rooms/{roomId}/image` | 채팅방 이미지 변경 |
| `DELETE` | `/api/chat/rooms/{roomId}` | 채팅방 퇴장 |
| `DELETE` | `/api/chat/rooms/{roomId}/members/{userId}` | 멤버 강퇴 (방장 전용) |
