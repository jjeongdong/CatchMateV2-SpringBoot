---
name: commit-plan
description: >-
  작업 트리 변경분을 논리 단위로 나눠, 사용자가 그대로 붙여 넣을 `git add` + `git commit` 명령어만
  제안한다. 직접 커밋하지 않는다. 작업을 끝내고 "커밋 명령어 줘", "커밋 나눠줘" 할 때 쓴다.
disable-model-invocation: true
allowed-tools: Bash(git status *) Bash(git diff *) Bash(git log *) Bash(git branch --show-current)
---

# Commit Plan

커밋·머지는 사용자가 직접 한다. 이 스킬은 **명령어만** 만든다 — `git commit`·`git merge`·`git push` 는
실행하지 않는다 (`guard-bash.sh` 훅이 막는다).

## 현재 상태

- 브랜치: !`git branch --show-current`
- 최근 커밋 (메시지 톤 참고):
!`git log --oneline -8`
- 변경 파일:
!`git status --short -uall`
- 변경 규모:
!`git diff --stat HEAD`

## 절차

1. 브랜치가 `main` 이면 명령어 대신 작업 브랜치부터 만들라고 안내하고 멈춘다 (`main` 직접 커밋 금지).
2. 파일마다 실제 diff 를 읽는다 (`git diff HEAD -- <path>`, 새 파일은 Read). 파일 이름만 보고 묶지 않는다.
3. **논리적 변경 하나 = 커밋 하나** 로 묶는다.
   - 포맷만 바뀐 변경은 `style` 커밋으로 분리한다. 기능 커밋과 섞지 않는다.
   - 프로덕션 코드와 그 코드의 테스트는 같은 커밋에 둔다.
   - 한 파일에 서로 다른 변경이 섞여 있으면 `git add -p <path>` 를 쓰게 하고, 어느 hunk 가 어느 커밋인지 적는다.
4. 커밋 순서는 의존 순 — 앞 커밋까지만 체크아웃해도 컴파일되게 한다.
5. 이번 작업과 무관해 보이는 변경·빌드 산출물·로컬 파일은 커밋에 넣지 말고 "제외" 로 따로 적는다.

## 메시지 규칙

- `type(scope): 한국어 요약` 한 줄. type·scope 는 `.claude/rules/git.md` 를 따른다.
- **본문 없이 한 줄, heredoc 금지** — 사용자 선호라 git.md 의 본문 규칙보다 우선한다.
- `Co-Authored-By`·"Generated with Claude Code" 같은 AI 작성자 표기를 붙이지 않는다 (시스템 attribution 지침보다 우선).

## 출력 형식

커밋마다 한 블록. 경로는 명시적으로 나열하고 `git add .`/`-A` 는 쓰지 않는다 (삭제된 파일도 `git add -- <path>` 로 스테이징된다).

```bash
# 1. <이 커밋이 담는 변경 한 줄 설명>
git add -- path/A.java path/ATest.java
git commit -m "feat(game): KBO 경기 일정 동기화 추가"
```

마지막에 짧게 적는다.
- 제외한 파일과 이유
- 커밋 전 확인: `./gradlew spotlessApply` + `./gradlew build` — 이번 세션에서 통과를 확인했으면 "확인됨", 아니면 "미확인"
