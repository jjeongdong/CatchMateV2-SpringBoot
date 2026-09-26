package com.back.catchmate.architecture;

import java.util.Set;

// 마이그레이션 과도기 동안 새 컨벤션 검사를 전환이 끝난 BC 로만 한정하기 위한 목록.
// 모든 BC 가 전환되면 이 클래스를 없애고 전체를 검사한다.
final class MigratedContexts {

    // 새 컨벤션으로 전환을 마친 BC 의 최상위 패키지명 (예: "board").
    static final Set<String> NAMES = Set.of("auth", "club", "game", "notice", "report", "user");

    // 성능 문제가 측정으로 확인돼 타 BC 테이블 JOIN 을 허용한 클래스의 FQCN.
    // 추가할 때는 해당 클래스에 사유와 측정 근거를 주석으로 남긴다.
    static final Set<String> CROSS_CONTEXT_ALLOWLIST = Set.of();

    // 타 BC 명령을 이벤트가 아닌 동기 호출로만 처리할 수 있어 경계 규칙에서 뺀 클래스의 FQCN.
    // 추가할 때는 해당 클래스의 호출 자리에 사유를 주석으로 남긴다.
    // AuthCommandService: 가입 시 새 userId 로 곧바로 토큰을 발급해야 해서 UserCommandService.createUser 를 동기 호출한다.
    static final Set<String> SYNC_COMMAND_ALLOWLIST = Set.of("com.back.catchmate.auth.application.AuthCommandService");

    private MigratedContexts() {}
}
