#!/bin/bash
# 되돌리기 어려운 명령만 실행 전에 막는다 — 메모리 당부는 Claude 가 잊을 수 있지만 훅은 매번 돈다
CMD=$(jq -r '.tool_input.command // empty')

deny() {
    echo "$1" >&2
    exit 2
}

# 커밋·머지·푸시는 사용자가 직접 한다
if echo "$CMD" | grep -qE '(^|[;&|(]|[[:space:]])git([[:space:]]+-[Cc][[:space:]]+[^[:space:]]+|[[:space:]]+--[^[:space:]]+)*[[:space:]]+(commit|merge|push)([[:space:]]|$)'; then
    deny "git commit/merge/push 는 사용자가 직접 한다. 실행하지 말고 명령어만 제안하라 (/commit-plan 형식)."
fi

# dev 프로필 datasource 가 운영 RDS(ddl-auto: update)라 접속 하나로 운영 데이터가 바뀔 수 있다
if echo "$CMD" | grep -q 'rds\.amazonaws\.com'; then
    deny "운영 RDS 에 직접 접속하지 않는다. 로컬 MySQL 이나 Testcontainers 를 써라."
fi

# 원격(ssh) 실행은 원격 설정을 따르므로 로컬 실행만 본다
if echo "$CMD" | grep -qE 'bootRun|java[[:space:]].*-jar' && ! echo "$CMD" | grep -q 'ssh'; then
    PROFILE=$(echo "$CMD" | grep -oE '(spring\.profiles\.active|SPRING_PROFILES_ACTIVE)[= ]+[A-Za-z0-9_,-]+' | tail -1 | sed -E 's/.*[= ]+//')
    # 명령에 프로필이 없으면 application.yml 기본값이 적용된다 — 기본값이 dev 인 시기도 있었다
    if [ -z "$PROFILE" ]; then
        PROFILE=$(sed -nE 's/^[[:space:]]+active:[[:space:]]*([^[:space:]#]+).*/\1/p' "$CLAUDE_PROJECT_DIR/src/main/resources/application.yml" | head -1)
    fi
    case ",$PROFILE," in
        *,dev,*) deny "dev 프로필은 운영 RDS 를 쓴다. --args='--spring.profiles.active=local' 처럼 local 프로필을 명시해 실행하라." ;;
    esac
fi

# 비용이 드는 인프라 생성은 막지 않되 매번 사용자에게 묻는다
if echo "$CMD" | grep -qE 'infra\.sh[[:space:]]+up([[:space:]]|$)'; then
    jq -n '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: "AWS 부하테스트 인프라를 띄운다 (약 $0.6/h 과금). 진행할지 확인"}}'
    exit 0
fi

exit 0
