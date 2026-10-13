#!/bin/bash
# main 에서 바로 고치면 "브랜치 작업 후 --no-ff 머지" 규칙을 지킬 수 없다. 당부 대신 편집 자체를 막는다
FILE=$(jq -r '.tool_input.file_path // empty')
case "$FILE" in
    "$CLAUDE_PROJECT_DIR"/*) ;;
    *) exit 0 ;;
esac

cd "$CLAUDE_PROJECT_DIR" || exit 0
[ "$(git branch --show-current)" = "main" ] || exit 0

# gitignore 대상(로컬 설정 등)은 커밋되지 않으므로 main 에서 고쳐도 된다
git check-ignore -q -- "$FILE" && exit 0

echo "main 브랜치에서는 편집하지 않는다. git switch -c {type}/{kebab-case} 로 작업 브랜치부터 만든 뒤 다시 편집하라." >&2
exit 2
