#!/usr/bin/env python3
"""
Stop 훅: Claude 가 응답을 마치고 턴을 넘기기 직전에 도는 '최종 검문소'.

위치/역할 (3단계 게이트의 마지막):
  - pretooluse-inject-rules.py  (PreToolUse)  = 규칙 주입.
  - posttooluse-validate-arch.py (PostToolUse) = 파일 1개 정적 검사(import/0-import).
  - 이 스크립트 (Stop) = 턴 끝에서 변경분 전체를 컴파일+archCheck 로 한 번에 검증.

왜 필요한가:
  PostToolUse 는 '방금 고친 그 파일 1개'만 본다. 그래서 멀티파일 작업에서
  A.java 시그니처 변경 → B.java 가 옛 시그니처 호출 같은 '합쳐야 드러나는'
  컴파일 깨짐을 못 잡는다. 이 훅이 턴 종료 시점에 ./gradlew compileJava archCheck
  를 한 번 돌려, 빌드가 깨진 채로 턴이 끝나는 것을 구조적으로 차단한다.

가드 (낭비/무한루프 방지):
  1. stop_hook_active == true  → 이미 이 훅 때문에 재진입한 것. 즉시 통과(무한루프 방지).
  2. 변경된 src/main/java/**/*.java 가 하나도 없으면 → 즉시 통과(분석/질문만 한 턴엔 빌드 안 돌림).

실패 시: {"decision":"block","reason":...} 로 gradle 출력 꼬리를 돌려보내
같은 턴에서 Claude 가 즉시 고치도록 유도한다. (성공/판단불가 → 흐름 안 막음)
"""
import json
import os
import subprocess
import sys

MAX_LOG_LINES = 60  # block reason 에 실을 gradle 출력 꼬리 길이


def project_dir(data: dict) -> str:
    return (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or data.get("cwd")
        or os.getcwd()
    )


def changed_main_java(cwd: str) -> bool:
    """git 기준 변경(스테이지/워킹/untracked)된 src/main/java 의 .java 가 있는가."""
    try:
        out = subprocess.run(
            ["git", "status", "--porcelain", "--untracked-files=all"],
            cwd=cwd, capture_output=True, text=True, timeout=15,
        )
    except Exception:
        # git 을 못 쓰면 게이트를 건너뛴다(흐름 우선).
        return False
    if out.returncode != 0:
        return False
    for line in out.stdout.splitlines():
        # 포맷: "XY path" 또는 "XY old -> new"(rename). 경로만 본다.
        path = line[3:].strip()
        if " -> " in path:
            path = path.split(" -> ", 1)[1]
        path = path.strip().strip('"').replace("\\", "/")
        if path.endswith(".java") and "src/main/java/" in path and "/build/" not in path:
            return True
    return False


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    # 가드 1: 이 훅이 block 해서 다시 들어온 경우 → 무한루프 방지.
    if data.get("stop_hook_active"):
        sys.exit(0)

    cwd = project_dir(data)

    # 가드 2: 변경된 main 소스가 없으면 빌드를 돌리지 않는다.
    if not changed_main_java(cwd):
        sys.exit(0)

    gradlew = os.path.join(cwd, "gradlew")
    if not os.path.exists(gradlew):
        sys.exit(0)

    try:
        result = subprocess.run(
            [gradlew, "compileJava", "archCheck", "--console=plain"],
            cwd=cwd, capture_output=True, text=True, timeout=300,
            env={**os.environ, "CLAUDE_PROJECT_DIR": cwd},
        )
    except subprocess.TimeoutExpired:
        # 타임아웃 시 막지 않는다(빌드 환경 문제일 수 있음). 사람이 판단.
        sys.exit(0)
    except Exception:
        sys.exit(0)

    if result.returncode == 0:
        sys.exit(0)  # 컴파일 + 아키텍처 통과 → 턴 종료 허용.

    combined = (result.stdout or "") + "\n" + (result.stderr or "")
    tail = "\n".join(combined.strip().splitlines()[-MAX_LOG_LINES:])
    reason = (
        "[Stop 게이트] 턴을 끝내기 전에 ./gradlew compileJava archCheck 가 실패했습니다. "
        "변경한 코드가 컴파일 에러이거나 헥사고날/0-import 규칙(.claude/ondemand-rules/*)을 "
        "위반했습니다. 아래 gradle 출력을 보고 고친 뒤 마무리하세요:\n\n```\n%s\n```"
        % tail
    )
    print(json.dumps({"decision": "block", "reason": reason}))
    sys.exit(0)


if __name__ == "__main__":
    main()
