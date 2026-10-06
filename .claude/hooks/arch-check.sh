#!/bin/bash
# 턴을 끝내기 전에 아키텍처 검사를 돌려, 규칙 위반을 사용자가 아니라 Claude 가 먼저 고치게 한다
INPUT=$(cat)
cd "$CLAUDE_PROJECT_DIR" || exit 0

if [ -z "$(git status --porcelain -uall -- 'src/*.java')" ]; then
    exit 0
fi

# 같은 코드 상태를 다시 검사하지 않도록 변경분 해시를 .git 아래에 남긴다
STATE_DIR="$(git rev-parse --git-dir)/claude-arch-check"
mkdir -p "$STATE_DIR"
HASH=$( { git diff HEAD -- src; git ls-files -o --exclude-standard -z -- src | xargs -0 cat 2>/dev/null; } | shasum | cut -d' ' -f1)

if [ "$HASH" = "$(cat "$STATE_DIR/passed" 2>/dev/null)" ]; then
    exit 0
fi

# 막은 뒤 코드가 그대로면 Claude 가 스스로 못 고친 것이다. 다시 막아 봐야 같은 실패만 반복되므로 사용자에게 넘긴다
if [ "$(echo "$INPUT" | jq -r '.stop_hook_active')" = "true" ] \
    && [ "$HASH" = "$(cat "$STATE_DIR/failed" 2>/dev/null)" ]; then
    exit 0
fi

if OUTPUT=$(./gradlew test --tests 'com.back.catchmate.architecture.*' -q 2>&1); then
    echo "$HASH" > "$STATE_DIR/passed"
    exit 0
fi

echo "$HASH" > "$STATE_DIR/failed"
{
    echo "아키텍처 검사(./gradlew test --tests 'com.back.catchmate.architecture.*')가 실패했다. 위반을 고친 뒤 끝내라."
    echo "$OUTPUT" | tail -30
    sed -n '/<failure/,/<\/failure>/p' build/test-results/test/TEST-com.back.catchmate.architecture.*.xml 2>/dev/null | head -60
} >&2
exit 2
