#!/usr/bin/env python3
"""컨텍스트 간 의존 그래프 · 순환 탐지 · 변환 순서 제안.

헥사고날 → MVC 전환에서 가장 큰 리스크는 FetchPort/FetchAdapter 간접층이
사라지면서 Service 가 Service 를 직접 주입할 때 **순환 의존**이 생기는 것이다.
Spring 은 생성자 주입 순환을 부팅 시점에 실패시키므로, 옮기고 나서 발견하면
이미 늦다. 이 스크립트는 옮기기 **전에** 그 순환을 예측한다.

사용법:
    python3 dep-graph.py                # 요약 + 순환 + 변환 순서 (기본)
    python3 dep-graph.py --edges        # 컨텍스트 쌍별 근거 파일까지
    python3 dep-graph.py --ctx board    # 한 컨텍스트의 in/out 상세
    python3 dep-graph.py --json         # 기계 판독용

출력의 두 그래프:
  [현재]  지금 소스의 컨텍스트 간 import 그래프 (FetchAdapter 경유 포함)
  [전환후] FetchAdapter 를 접었을 때 남는 Service→Service 그래프
          = MVC 로 옮기면 실제로 Spring 이 보게 될 그래프
"""
import argparse
import json
import os
import re
import sys
from collections import defaultdict

ROOT = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
SRC = os.path.join(ROOT, "src", "main", "java", "com", "back", "catchmate")
BASE_PKG = "com.back.catchmate"

# 컨텍스트가 아닌 공용 패키지 — 그래프에서 제외한다 (모두가 의존하는 것이 정상)
NON_CONTEXT = {"global", "common"}

IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?(" + re.escape(BASE_PKG) + r"\.[\w.]+)\s*;")


def contexts():
    if not os.path.isdir(SRC):
        sys.exit(f"[dep-graph] 소스 디렉토리 없음: {SRC}")
    out = []
    for name in sorted(os.listdir(SRC)):
        if os.path.isdir(os.path.join(SRC, name)) and name not in NON_CONTEXT:
            out.append(name)
    return out


def java_files(ctx):
    base = os.path.join(SRC, ctx)
    for dirpath, _, filenames in os.walk(base):
        for fn in filenames:
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def rel(path):
    return os.path.relpath(path, ROOT)


def is_fetch_adapter(path):
    return os.path.basename(path).endswith("FetchAdapter.java")


def classify(symbol, path):
    """간선 하나가 MVC 전환 후 **어떤 종류의 의존**이 되는지 판정한다.

    Spring 부팅을 깨뜨리는 것은 생성자 주입 순환뿐이다. 이벤트 구독이나 단순
    타입 참조는 순환처럼 보여도 빈 그래프에 순환을 만들지 않으므로, 이 셋을
    섞어서 순환을 판정하면 있지도 않은 순환을 보고하게 된다.

      injection — 상대 컨텍스트의 정문(UseCase)을 주입받는다.
                  MVC 에선 `private final XxxService` 가 되므로 **순환 판정 대상**.
      event     — 이벤트 클래스를 참조하는 구독자.
                  ApplicationEventPublisher 를 사이에 두므로 빈 순환이 아니다.
      type      — DTO/enum 등 타입만 참조. 컴파일 의존이지 빈 의존이 아니다.
    """
    if symbol.endswith("UseCase"):
        return "injection"
    if symbol.endswith("Event") and os.path.basename(path).endswith("EventListener.java"):
        return "event"
    return "type"


def scan():
    """returns edges_current, edges_projected

    각 edge: (src_ctx, dst_ctx) -> list of (file, symbol, is_adapter, kind)
    """
    current = defaultdict(list)
    projected = defaultdict(list)
    ctxs = set(contexts())

    for ctx in sorted(ctxs):
        for path in java_files(ctx):
            adapter = is_fetch_adapter(path)
            with open(path, encoding="utf-8", errors="replace") as fh:
                for line in fh:
                    if line.startswith(("class ", "public ", "@")):
                        # import 블록은 클래스 선언 전에 끝난다 — 조기 종료로 스캔 비용 절감
                        if not line.startswith("@"):
                            break
                    m = IMPORT_RE.match(line)
                    if not m:
                        continue
                    fqcn = m.group(1)
                    parts = fqcn[len(BASE_PKG) + 1:].split(".")
                    if not parts:
                        continue
                    other = parts[0]
                    if other in NON_CONTEXT or other == ctx or other not in ctxs:
                        continue
                    symbol = fqcn.rsplit(".", 1)[-1]
                    kind = classify(symbol, path)
                    current[(ctx, other)].append((rel(path), symbol, adapter, kind))
                    # 전환 후 그래프: FetchAdapter 가 들고 있던 의존은 사라지지 않고
                    # 그 컨텍스트의 Service 로 그대로 이관된다 → 같은 간선으로 접는다.
                    projected[(ctx, other)].append((rel(path), symbol, adapter, kind))
    return current, projected


def build_adj(edges, kinds=None):
    """kinds 를 주면 해당 종류의 참조가 하나라도 있는 간선만 남긴다."""
    adj = defaultdict(set)
    for (a, b), refs in edges.items():
        if kinds is None or any(r[3] in kinds for r in refs):
            adj[a].add(b)
    return adj


def find_cycles(adj, nodes):
    """Tarjan SCC — 크기 2 이상인 SCC 와 자기 루프를 순환으로 본다."""
    index = {}
    low = {}
    stack = []
    on_stack = set()
    counter = [0]
    sccs = []

    def strongconnect(v):
        # 재귀 대신 명시적 스택 (컨텍스트 수는 적지만 습관적으로 안전하게)
        work = [(v, iter(sorted(adj.get(v, ()))))]
        index[v] = low[v] = counter[0]
        counter[0] += 1
        stack.append(v)
        on_stack.add(v)
        while work:
            node, it = work[-1]
            advanced = False
            for w in it:
                if w not in index:
                    index[w] = low[w] = counter[0]
                    counter[0] += 1
                    stack.append(w)
                    on_stack.add(w)
                    work.append((w, iter(sorted(adj.get(w, ())))))
                    advanced = True
                    break
                elif w in on_stack:
                    low[node] = min(low[node], index[w])
            if advanced:
                continue
            work.pop()
            if work:
                parent = work[-1][0]
                low[parent] = min(low[parent], low[node])
            if low[node] == index[node]:
                comp = []
                while True:
                    w = stack.pop()
                    on_stack.discard(w)
                    comp.append(w)
                    if w == node:
                        break
                sccs.append(comp)

    for v in sorted(nodes):
        if v not in index:
            strongconnect(v)

    cycles = []
    for comp in sccs:
        if len(comp) > 1:
            cycles.append(sorted(comp))
        elif comp and comp[0] in adj.get(comp[0], ()):
            cycles.append(comp)
    return cycles


def cycle_path(adj, comp):
    """SCC 안에서 **실제로 존재하는** 순환 경로 하나를 찾아 반환한다.

    SCC 멤버 목록을 정렬해 이어 붙이면 간선이 없는 구간까지 화살표로 이어져
    있지도 않은 경로를 보고하게 된다. 사용자가 끊어야 할 간선을 짚으려면
    경로가 진짜여야 한다.
    """
    members = set(comp)
    start = sorted(members)[0]
    path = []
    seen = set()

    def dfs(node):
        path.append(node)
        seen.add(node)
        for nxt in sorted(adj.get(node, ()) & members):
            if nxt == start and len(path) > 1:
                return True
            if nxt not in seen and dfs(nxt):
                return True
        path.pop()
        seen.discard(node)
        return False

    if len(comp) == 1:
        return list(comp) if start in adj.get(start, ()) else list(comp)
    return path if dfs(start) else sorted(comp)


def migration_order(adj, nodes, cycles):
    """의존이 적은 leaf 부터 나오는 순서.

    순환에 속한 노드(in_cycle)와, 순환에 의존해서 덩달아 막힌 노드(downstream)를
    구분해 돌려준다. 둘을 뭉뚱그리면 "admin 도 순환이다" 라고 오해하게 된다.
    """
    in_cycle = {n for c in cycles for n in c}
    remaining = set(nodes)
    order = []
    while remaining:
        ready = sorted(
            n for n in remaining
            if n not in in_cycle and not (adj.get(n, set()) & remaining)
        )
        if not ready:
            break
        order.extend(ready)
        remaining -= set(ready)
    return order, sorted(remaining & in_cycle), sorted(remaining - in_cycle)


def fmt_ctx_stats(ctx, adj, radj, file_counts):
    outs = sorted(adj.get(ctx, ()))
    ins = sorted(radj.get(ctx, ()))
    return f"{ctx:<14} 파일 {file_counts[ctx]:>3}  →의존 {len(outs):>2} [{', '.join(outs) or '-'}]  ←피의존 {len(ins):>2} [{', '.join(ins) or '-'}]"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--edges", action="store_true", help="간선별 근거 파일 출력")
    ap.add_argument("--ctx", help="한 컨텍스트 상세")
    ap.add_argument("--json", action="store_true", help="JSON 출력")
    args = ap.parse_args()

    ctxs = contexts()
    file_counts = {c: sum(1 for _ in java_files(c)) for c in ctxs}
    _current, projected = scan()

    # 순환은 **주입 간선만으로** 판정한다 (classify() 주석 참조).
    adj_inj = build_adj(projected, kinds={"injection"})
    adj_all = build_adj(projected)
    radj_all = defaultdict(set)
    for (a, b) in projected:
        radj_all[b].add(a)

    cycles_inj = find_cycles(adj_inj, ctxs)
    cycles_all = find_cycles(adj_all, ctxs)
    order, cyc_nodes, downstream = migration_order(adj_inj, ctxs, cycles_inj)

    if args.json:
        def edge_kinds(refs):
            k = defaultdict(int)
            for r in refs:
                k[r[3]] += 1
            return dict(k)
        print(json.dumps({
            "contexts": ctxs,
            "file_counts": file_counts,
            "edges": {f"{a}->{b}": edge_kinds(v) for (a, b), v in sorted(projected.items())},
            "cycles_injection": cycles_inj,
            "cycles_including_type_refs": cycles_all,
            "migration_order": order,
            "in_cycle": cyc_nodes,
            "blocked_downstream": downstream,
        }, ensure_ascii=False, indent=2))
        return 0

    if args.ctx:
        ctx = args.ctx
        if ctx not in ctxs:
            sys.exit(f"[dep-graph] 알 수 없는 컨텍스트: {ctx} (가능: {', '.join(ctxs)})")
        print(f"=== {ctx} 상세 ===\n")
        print(f"-- {ctx} 가 의존하는 컨텍스트 --")
        for other in sorted(adj_all.get(ctx, ())):
            refs = projected[(ctx, other)]
            inj = sum(1 for r in refs if r[3] == "injection")
            print(f"  → {other}  (참조 {len(refs)}건, 주입 {inj}건)")
            for f, sym, _is_ad, kind in sorted(set(refs)):
                print(f"      [{kind:<9}] {sym:<34} {f}")
        print(f"\n-- {ctx} 에 의존하는 컨텍스트 --")
        for other in sorted(radj_all.get(ctx, ())):
            refs = projected[(other, ctx)]
            inj = sum(1 for r in refs if r[3] == "injection")
            print(f"  ← {other}  (참조 {len(refs)}건, 주입 {inj}건)")
        return 0

    print("=" * 78)
    print(f"컨텍스트 {len(ctxs)}개 · 프로덕션 Java {sum(file_counts.values())}개")
    print("=" * 78)
    print("  (→의존 = 주입 간선 기준. 이벤트 구독·타입 참조는 빈 순환을 만들지 않는다)")
    print()
    for c in sorted(ctxs, key=lambda x: (len(adj_inj.get(x, ())), file_counts[x])):
        print("  " + fmt_ctx_stats(c, adj_inj, radj_all, file_counts))
    print()

    print("-" * 78)
    print("전환 후(Service→Service) 생성자 주입 순환")
    print("-" * 78)
    if not cycles_inj:
        print("  없음 — 주입 간선만 보면 순환이 아니다.")
        print("  FetchAdapter 를 걷어내고 Service 를 그대로 주입해도 Spring 부팅은 깨지지 않는다.")
    else:
        print("  ⚠️ 아래 묶음은 MVC 로 옮기는 순간 생성자 주입 순환이 된다.")
        print("     옮기기 전에 끊는 방법을 먼저 정해야 한다 (이벤트 발행 / 조회 전용 분리 /")
        print("     한쪽을 상위 조립 서비스로 승격 / 마지막 수단 @Lazy).")
        for cyc in cycles_inj:
            path = cycle_path(adj_inj, cyc)
            print(f"\n     ● 묶음: {', '.join(sorted(cyc))}")
            print(f"       실제 고리: {' → '.join(path)} → {path[0]}")
            print("       이 묶음 안의 주입 간선 (하나를 끊으면 고리가 풀린다):")
            members = set(cyc)
            for (a, b), refs in sorted(projected.items()):
                if a not in members or b not in members:
                    continue
                inj = [r for r in refs if r[3] == "injection"]
                if not inj:
                    continue
                print(f"        {a} → {b}: 주입 {len(inj)}건")
                for f, sym, _is_ad, _k in sorted(set(inj))[:6]:
                    print(f"           {sym:<34} {f}")
                if len(set(inj)) > 6:
                    print(f"           … 외 {len(set(inj)) - 6}건")

    extra = [c for c in cycles_all if c not in cycles_inj]
    if extra:
        print()
        print("  참고 — 이벤트·타입 참조까지 포함하면 아래도 고리를 이룬다.")
        print("  빈 순환은 아니지만 **패키지 순환**이라, DTO 를 합칠 때 이름·소유권 충돌이 나기 쉽다.")
        for cyc in extra:
            print(f"     ○ {' → '.join(cyc)} → {cyc[0]}")
    print()

    print("-" * 78)
    print("변환 순서 제안 (의존이 없는 leaf 부터)")
    print("-" * 78)
    if order:
        for i, c in enumerate(order, 1):
            print(f"  {i:>2}. {c:<14} (파일 {file_counts[c]}, 주입 의존 {len(adj_inj.get(c, ()))})")
    if cyc_nodes:
        print(f"\n  ⚠️ 순환 당사자 (고리를 먼저 끊어야 함): {', '.join(cyc_nodes)}")
    if downstream:
        print(f"  ⏸  위 순환에 의존해 덩달아 대기: {', '.join(downstream)}")
        print("     → 순환 자체는 아니다. 순환이 풀리면 이 순서대로 이어서 옮길 수 있다.")
    print()

    if args.edges:
        print("-" * 78)
        print("간선 상세")
        print("-" * 78)
        for (a, b), refs in sorted(projected.items()):
            inj = sum(1 for r in refs if r[3] == "injection")
            print(f"\n  {a} → {b}  (참조 {len(refs)}건 / 주입 {inj}건)")
            for f, sym, _is_ad, kind in sorted(set(refs)):
                print(f"    [{kind:<9}] {sym:<34} {f}")

    return 1 if cycles_inj else 0


if __name__ == "__main__":
    sys.exit(main())
