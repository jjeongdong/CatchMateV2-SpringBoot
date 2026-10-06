#!/bin/bash
# 편집한 파일만 포맷한다 — 전체 spotlessApply 는 느려서 매 편집마다 돌릴 수 없다
FILE=$(jq -r '.tool_input.file_path // empty')
case "$FILE" in
    "$CLAUDE_PROJECT_DIR"/src/*.java) ;;
    *) exit 0 ;;
esac

# 편집 도중 문법이 깨진 상태면 포맷이 실패한다. 그건 컴파일·아키텍처 검사가 잡으므로 여기선 막지 않는다
cd "$CLAUDE_PROJECT_DIR" && ./gradlew spotlessApply -PspotlessIdeHook="$FILE" -q >/dev/null 2>&1
exit 0
