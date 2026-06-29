#!/usr/bin/env python3
"""
SessionStart 훅: 세션이 시작될 때마다 CLAUDE.md '절대 변경 금지' 핵심 3개만
항상 주입한다. (Java 파일을 안 건드리는 분석·질문 세션에서도 함정을 알도록)

pretooluse-inject-rules.py 와의 분담:
  - 이 훅 (SessionStart) = 위험한 '단순화 금지' 가드레일 3줄만, 모든 세션에 무조건.
    (~200토큰. Java 작업 없는 세션에서도 '합치면 부팅 깨짐' 같은 지뢰를 회피.)
  - pretooluse-inject-rules.py (PreToolUse) = 상세 규칙 8K 토큰 전체를, Java 파일을
    실제로 Read/Edit 할 때 1회만. (상세는 여전히 on-demand → 토큰 절약 유지.)

설계 의도: 가장 위험한 건 'CLAUDE.md 절대 변경 금지' 3개다. 이건 "단순화하자"는
제안이 나오기 쉬운 곳이라, Java 를 안 여는 대화에서도 모델이 항상 알고 있어야 한다.
상세 규칙(네이밍·의존성 방향 등)은 비싸므로 여기 싣지 않고 PreToolUse 에 맡긴다.
"""
import json
import sys

GUARDRAILS = """[하네스 자동 주입 · 세션 상시] 이 프로젝트의 **절대 변경 금지(단순화 X)** 3가지입니다. \
Java 를 수정하지 않는 대화에서도 아래를 어기는 제안(합치기/단순화/삭제)을 하지 마세요. \
상세 규칙은 Java 파일을 열 때 자동 주입됩니다. SSOT: CLAUDE.md + .claude/ondemand-rules/*.

1. **이중 단계 이벤트 리스너 (Transactional Outbox)** — `@EventListener`(커밋 전 Outbox DB 저장) \
+ `@TransactionalEventListener(AFTER_COMMIT)`(커밋 후 FCM 발송) + `NotificationScheduler`(60초 재시도). \
이 3단계를 합치거나 단순화하지 말 것. (외부 호출은 AFTER_COMMIT + @Async 에만.)

2. **RedisNotificationPublisher 분리** — `RedisNotificationPublisher`(NotificationDispatchPort 구현)와 \
`ChatMessageRedisPublisher`(@TransactionalEventListener)는 의도적 분리. 합치면 JDK 동적 프록시에서 \
메서드가 사라져 Spring 부팅이 깨짐. 한쪽으로 합치거나 인터페이스 메서드를 지우지 말 것.

3. **Soft Delete (엔티티 성격별)** — 핵심 애그리거트(User·Board·ChatRoom·ChatMessage)만 \
`deletedAt`+`@SQLRestriction`+도메인 `delete()`. 이 엔티티엔 `deleteById`/물리삭제 금지. \
반대로 조인·토글·토큰·아웃박스 엔티티(Bookmark·Block·Enroll·ChatRoomMember·RefreshToken· \
NotificationOutbox 등)는 물리삭제가 정상. ('모든 엔티티 soft-delete' 아님.)"""


def main() -> None:
    # 입력 파싱 실패해도 흐름을 막지 않는다.
    try:
        json.load(sys.stdin)
    except Exception:
        pass

    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "SessionStart",
            "additionalContext": GUARDRAILS,
        }
    }))
    sys.exit(0)


if __name__ == "__main__":
    main()
