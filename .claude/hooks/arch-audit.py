#!/usr/bin/env python3
"""
아키텍처 휴리스틱 감사 (advisory) — posttooluse-validate-arch.py 의 자매 스크립트.

위치/역할:
  - posttooluse-validate-arch.py = 결정론 게이트(①). import 방향·0-import·예외삼킴만,
    오탐 0, 빌드 차단(decision=block / archCheck 실패).
  - 이 스크립트 = 휴리스틱 리포트(②). 코드 의미를 봐야 확정되는 규칙을 텍스트 패턴으로
    "용의자"만 추려낸다. **오탐이 있을 수 있고, 빌드를 막지 않는다(advisory).**
    그래서 ① 과 의도적으로 분리한다 (합치면 ① 의 '오탐 0' 원칙이 깨짐).

출력:
  1. 컨텍스트별 구조 적용 매트릭스 (FetchPort/Adapter/정문/Event/Listener)
  2. 휴리스틱 위반 후보 (사람이 최종 판정):
     [A] 금지 접미사 / FetchAdapter 의 Client 정문 진입
     [B] 이벤트 record 의 구독자 관심사 누수(recipient/fcmToken/외부 List)
     [C] 발송(Dispatch) service 의 @Transactional (어노테이션만, 주석 제외)
     [F] 물리 삭제 흔적 (deleteById / delete from / @Modifying delete)
     [G] BaseException 아닌 generic 예외 throw

사용:
  python3 .claude/hooks/arch-audit.py                 # 전체(14개 컨텍스트) 리포트
  python3 .claude/hooks/arch-audit.py board           # board 컨텍스트만
  python3 .claude/hooks/arch-audit.py board,enroll    # 여러 개(쉼표 구분)
  python3 .claude/hooks/arch-audit.py --context board # --context/-c 플래그도 동일
  python3 .claude/hooks/arch-audit.py --strict        # 후보가 있으면 종료코드 1 (선택적 CI 용)

규칙 SSOT: .claude/ondemand-rules/*.md
종료코드: 기본 0(advisory). --strict 일 때만 후보 발견 시 1.
"""
import os
import re
import sys

BASE_PKG = "com.back.catchmate"
SHARED = {"global", "common"}

# soft-delete 정책은 '엔티티 성격별'(SSOT: backend-patterns.md). 핵심 애그리거트만 @SQLRestriction
# 으로 soft-delete 하고, 조인/토글·토큰·아웃박스 엔티티는 물리삭제가 정상. 따라서 [F] 는 이름
# whitelist 대신 "@SQLRestriction 가진 엔티티의 물리삭제"만 위반 후보로 본다 (엔티티-인지).


def project_dir() -> str:
    return os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()


def src_root(pd: str) -> str:
    return os.path.join(pd, "src", "main", "java", *BASE_PKG.split("."))


def java_files(root: str):
    for dirpath, _dirs, files in os.walk(root):
        if "/build/" in dirpath.replace("\\", "/"):
            continue
        for fn in sorted(files):
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def read(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return fh.read()
    except OSError:
        return ""


def strip_comments(text: str) -> str:
    """블록(/* */)·라인(//) 주석 제거. 휴리스틱이 주석을 오탐하지 않도록."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return "\n".join(l for l in text.splitlines() if not l.lstrip().startswith("//"))


def rel(pd: str, path: str) -> str:
    return os.path.relpath(path, pd)


def contexts(root: str):
    try:
        return sorted(
            n for n in os.listdir(root)
            if os.path.isdir(os.path.join(root, n)) and n not in SHARED
        )
    except OSError:
        return []


DELETE_REPO_VAR = re.compile(r"\bjpa([A-Z]\w+?)Repository\b")


def soft_delete_entities(root: str):
    """@SQLRestriction 을 가진 엔티티의 도메인명 집합 (예: BoardEntity → 'Board')."""
    s = set()
    for f in java_files(root):
        b = os.path.basename(f)
        if b.endswith("Entity.java") and "@SQLRestriction" in read(f):
            s.add(b[:-len("Entity.java")])
    return s


def entity_of_delete(path: str, line: str):
    """물리삭제 줄이 어떤 엔티티를 지우는지 추정. jpaXxxRepository → Xxx, 아니면 파일명."""
    m = DELETE_REPO_VAR.search(line)
    if m:
        return m.group(1)
    m = re.match(r"([A-Z]\w+?)Repository", os.path.basename(path))
    return m.group(1) if m else None


# ─────────────────────────────────────────────────────────────────────────────
# 1. 구조 매트릭스
# ─────────────────────────────────────────────────────────────────────────────
def matrix(root: str, ctxs):
    print("=" * 72)
    print("① 컨텍스트별 구조 적용 매트릭스 (%d개)" % len(ctxs))
    print("=" * 72)
    print("%-13s%9s%10s%12s%7s%9s" %
          ("ctx", "FetchPort", "FetchAdpt", "정문(Int/Adm)", "Event", "Listener"))
    issues = []
    for c in ctxs:
        cdir = os.path.join(root, c)
        all_java = list(java_files(cdir))

        def count(pred):
            return sum(1 for f in all_java if pred(os.path.basename(f), f))

        fp = count(lambda n, f: n.endswith("FetchPort.java"))
        fa = count(lambda n, f: n.endswith("FetchAdapter.java"))
        gate = count(lambda n, f: n.endswith("UseCase.java")
                     and ("Internal" in n or "Admin" in n))
        ev = count(lambda n, f: "/application/event/" in f.replace("\\", "/"))
        li = count(lambda n, f: n.endswith("EventListener.java")
                   and "/adapter/in/event/" in f.replace("\\", "/"))
        print("%-13s%9d%10d%12d%7d%9d" % (c, fp, fa, gate, ev, li))
        if fp != fa:
            issues.append("  ⚠️ %s: FetchPort(%d) != FetchAdapter(%d) — 짝이 안 맞음 (구현 누락 의심)"
                          % (c, fp, fa))
    if issues:
        print("\n[매트릭스 경고]")
        print("\n".join(issues))
    return len(issues)


# ─────────────────────────────────────────────────────────────────────────────
# 2. 휴리스틱 위반 후보
# ─────────────────────────────────────────────────────────────────────────────
GENERIC_THROW = re.compile(
    r"throw\s+new\s+(RuntimeException|IllegalStateException|IllegalArgumentException|Exception)\s*\(")
PHYSICAL_DELETE = re.compile(r"(?i)\bdelete\s+from\b|\bdeleteBy[A-Z]\w*|\.deleteById\s*\(|\.deleteAll\s*\(")
EVENT_LEAK = re.compile(r"\brecipientId\b|\bfcmToken\b|List<\s*[A-Z]\w*User\b|List<\s*[A-Z]\w*Token\b")
ANNOTATION_TX = re.compile(r"^\s*@Transactional\b")
CLIENT_USECASE_IMPORT = re.compile(r"import\s+[\w.]+\.application\.port\.in\.\w*Client\w*UseCase\s*;")
FORBIDDEN_SUFFIX = re.compile(r"\bclass\s+\w+(Orchestrator|Facade|ApplicationService)\b")


def check_heuristics(pd: str, scan_dirs, soft_entities):
    findings = {"A": [], "B": [], "C": [], "F": [], "G": []}

    seen = []
    for d in scan_dirs:
        seen.extend(java_files(d))
    for f in seen:
        raw = read(f)
        if not raw:
            continue
        code = strip_comments(raw)
        rp = rel(pd, f)
        base = os.path.basename(f)
        norm = f.replace("\\", "/")

        # [A] 금지 접미사
        m = FORBIDDEN_SUFFIX.search(code)
        if m:
            findings["A"].append("%s :: 금지 접미사 클래스명 (%s)" % (rp, m.group(1)))

        # [A] FetchAdapter 가 Client 정문으로 진입
        if base.endswith("FetchAdapter.java") and CLIENT_USECASE_IMPORT.search(code):
            findings["A"].append("%s :: FetchAdapter 가 Client UseCase 진입 (Internal/Admin 으로)" % rp)

        # [B] 이벤트 record 페이로드 누수
        if "/application/event/" in norm:
            for i, ln in enumerate(code.splitlines(), 1):
                if EVENT_LEAK.search(ln):
                    findings["B"].append("%s:%d :: %s" % (rp, i, ln.strip()[:70]))

        # [C] 발송 service 의 @Transactional (어노테이션 줄만)
        if base.endswith("DispatchService.java"):
            for i, ln in enumerate(code.splitlines(), 1):
                if ANNOTATION_TX.match(ln):
                    findings["C"].append("%s:%d :: 발송 service 에 @Transactional" % (rp, i))

        # [F] 물리 삭제 흔적 — soft-delete 엔티티(@SQLRestriction)를 지우는 경우만 위반.
        #     raw 라인 기준(정확한 line no.). 주석 라인은 건너뛰고, 같은 줄에 마커
        #     'arch-audit:allow-hard-delete' 가 있으면 의도된 예외로 허용.
        for i, ln in enumerate(raw.splitlines(), 1):
            s = ln.lstrip()
            if s.startswith("//") or s.startswith("*"):
                continue
            if PHYSICAL_DELETE.search(ln):
                if "arch-audit:allow-hard-delete" in ln:
                    continue
                ent = entity_of_delete(f, ln)
                if ent and ent in soft_entities:
                    findings["F"].append("%s:%d :: [%s=soft-delete] %s"
                                         % (rp, i, ent, ln.strip()[:58]))

        # [G] generic 예외 throw
        for i, ln in enumerate(code.splitlines(), 1):
            if GENERIC_THROW.search(ln):
                findings["G"].append("%s:%d :: %s" % (rp, i, ln.strip()[:70]))

    return findings


HEADERS = {
    "A": "[A] 정문/네이밍 — 금지 접미사 · FetchAdapter 의 Client 진입 (architecture #2, conventions #1)",
    "B": "[B] 이벤트 페이로드 누수 — 구독자 관심사(recipient/fcmToken/외부 List) (patterns #4·#5)",
    "C": "[C] 발송 service @Transactional — 외부호출 중 커넥션 점유 (conventions #5)",
    "F": "[F] soft-delete 엔티티의 물리 삭제 — @SQLRestriction 엔티티만 (patterns Soft Delete)",
    "G": "[G] generic 예외 — BaseException(ErrorCode) 아닌 throw (conventions #4)",
}


def report_heuristics(findings) -> int:
    print("\n" + "=" * 72)
    print("② 휴리스틱 위반 후보 (⚠️ = 사람이 최종 판정. 오탐 가능)")
    print("=" * 72)
    total = 0
    for key in ("A", "B", "C", "F", "G"):
        hits = findings[key]
        print("\n" + HEADERS[key])
        if not hits:
            print("  ✅ 후보 없음")
            continue
        total += len(hits)
        for h in hits:
            print("  ⚠️ " + h)
    return total


def parse_selected(argv):
    """인자에서 컨텍스트 필터를 추출. 없으면 None(전체)."""
    names = []
    i = 1
    while i < len(argv):
        a = argv[i]
        if a in ("--context", "-c") and i + 1 < len(argv):
            names.extend(argv[i + 1].split(","))
            i += 2
            continue
        if a.startswith("-"):  # --strict 등 다른 플래그
            i += 1
            continue
        names.extend(a.split(","))  # 위치 인자 = 컨텍스트
        i += 1
    return [n.strip() for n in names if n.strip()] or None


def main() -> None:
    strict = "--strict" in sys.argv
    pd = project_dir()
    root = src_root(pd)
    if not os.path.isdir(root):
        print("[arch-audit] 소스 루트 없음: %s" % root)
        sys.exit(0)

    all_ctxs = contexts(root)
    selected = parse_selected(sys.argv)
    if selected:
        unknown = [n for n in selected if n not in all_ctxs]
        if unknown:
            print("[arch-audit] 알 수 없는 컨텍스트: %s" % ", ".join(unknown))
            print("[arch-audit] 사용 가능: %s" % ", ".join(all_ctxs))
            sys.exit(2)
        ctxs = [c for c in all_ctxs if c in selected]
        scan_dirs = [os.path.join(root, c) for c in ctxs]
        print("[arch-audit] 대상 컨텍스트: %s\n" % ", ".join(ctxs))
    else:
        ctxs = all_ctxs
        scan_dirs = [root]

    mat_issues = matrix(root, ctxs)
    soft_entities = soft_delete_entities(root)
    findings = check_heuristics(pd, scan_dirs, soft_entities)
    total = report_heuristics(findings)

    print("\n" + "=" * 72)
    print("요약: 구조 경고 %d건 · 휴리스틱 후보 %d건" % (mat_issues, total))
    print("이 리포트는 advisory 입니다. 확정 위반(빌드 차단)은 ./gradlew archCheck 가 담당.")
    print("후보를 검토해 실제 위반이면 수정, 정당한 예외면 무시(또는 whitelist).")
    print("=" * 72)

    sys.exit(1 if (strict and (total or mat_issues)) else 0)


if __name__ == "__main__":
    main()
