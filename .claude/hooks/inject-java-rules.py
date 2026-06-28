#!/usr/bin/env python3
"""
PreToolUse 훅: Java 파일을 수정/생성/조회할 때, 백엔드 규칙(SSOT) 3종을
컨텍스트에 1회 주입한다. (glob 트리거의 의도를 Claude Code 훅으로 구현)

동작:
  - tool_input 의 file_path 가 .java 가 아니면 아무것도 안 함 (즉시 종료).
  - .java 면 세션당 1회만 .claude/ondemand-rules/*.md 전체를 additionalContext 로 주입.
  - 세션 식별은 session_id, 중복 주입 방지는 TMPDIR 의 마커 파일로.

설계 의도: 룰을 매 세션 무조건 로드(~8K 토큰)하지 않고, 실제 Java 작업이
시작될 때만 비용을 낸다. 단일 출처(SSOT)는 여전히 .md 파일 하나뿐.
"""
import json
import os
import sys

RULE_FILES = [
    "backend-architecture.md",
    "backend-coding-conventions.md",
    "backend-patterns.md",
]


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        # 입력 파싱 실패 시 도구 흐름을 막지 않는다.
        sys.exit(0)

    tool_input = data.get("tool_input") or {}
    path = tool_input.get("file_path") or tool_input.get("path") or ""
    if not isinstance(path, str) or not path.endswith(".java"):
        sys.exit(0)

    session_id = data.get("session_id") or "nosession"
    marker_dir = os.path.join(os.environ.get("TMPDIR", "/tmp"), "claude-java-rules")
    marker = os.path.join(marker_dir, session_id)
    try:
        os.makedirs(marker_dir, exist_ok=True)
    except Exception:
        pass

    # 이번 세션에서 이미 주입했으면 조용히 종료.
    if os.path.exists(marker):
        sys.exit(0)

    project_dir = (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or data.get("cwd")
        or os.getcwd()
    )
    rules_dir = os.path.join(project_dir, ".claude", "ondemand-rules")

    parts = []
    for name in RULE_FILES:
        fp = os.path.join(rules_dir, name)
        try:
            with open(fp, "r", encoding="utf-8") as fh:
                parts.append("===== {} =====\n{}".format(name, fh.read()))
        except FileNotFoundError:
            continue

    if not parts:
        # 룰 파일을 못 찾으면 흐름을 막지 않는다.
        sys.exit(0)

    context = (
        "[하네스 자동 주입] 이 프로젝트의 백엔드 작업 필수 규칙(SSOT)입니다. "
        "지금부터 Java 파일을 수정/생성할 때 아래 규칙(헥사고날 의존성·0-import·"
        "Outbox/이벤트 2단계·네이밍·트랜잭션 경계)을 반드시 준수하세요.\n\n"
        + "\n\n".join(parts)
    )

    # 주입 성공 후 마커 기록 (실패해도 무방).
    try:
        open(marker, "w").close()
    except Exception:
        pass

    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "additionalContext": context,
        }
    }))
    sys.exit(0)


if __name__ == "__main__":
    main()
