# Phase 16: Command evidence (D-07, D-08)

- **Base commit:** `505e49819fba7b2ba980a718d9ec6b66b55027b0` (last pre-Phase-16 change to the docs; every "unchanged" comparison is against it).
- **Shell:** macOS zsh, BSD `grep` and `wc`. `wc -l` pads its count with spaces; the outputs below keep the padding and are compared as integers.
- **Date (UTC):** 2026-10-10.
- **Rule:** a `Matches text: no` verdict means the doc text gets fixed, never the evidence. Entries that back no doc text carry `Matches text: n/a (gate check)`.

### E-01: `@Disabled` total

Backs: `docs/FORK-DELTA.md`, "Named skip inventory", the first `text` block line: "-> 7 annotation sites, 7 test methods".

```sh
grep -rho '@Disabled' src/test/java | wc -l
```

```text
       7
```

Matches text: yes

### E-02: `Assumptions.assumeTrue` total, and the empty case

Backs: `docs/FORK-DELTA.md`, "Named skip inventory", the second `text` block line: "-> 1 site, in a parameterized test with 11 cases". The empty case shows the command prints a number even with zero matches.

```sh
grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l
grep -rho NoSuchTokenP16 src/test/java | wc -l
```

```text
       1
       0
```

Matches text: yes

### E-03: the four `git diff --shortstat` commands beneath the raw-numbers table

Backs: `docs/FORK-DELTA.md`, "The raw numbers", the rewritten last sentence of the first paragraph: "The four commands shown beneath the table reproduce them against the `v1.7.0_gg1` tag." Table rows: 84/9476/451, 95/13587/484, 30/1368/98 and 31/1826/130.

```sh
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src build.gradle.kts gradle .github
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src/main
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src/main
```

```text
 84 files changed, 9476 insertions(+), 451 deletions(-)
 95 files changed, 13587 insertions(+), 484 deletions(-)
 30 files changed, 1368 insertions(+), 98 deletions(-)
 31 files changed, 1826 insertions(+), 130 deletions(-)
```

Matches text: yes

### E-04: publication gate dry run on the IN-09 tip

Backs: no doc text. It proves the plan 16-03 merge and allow-list procedure by command on the real repository, with no ref moved. X is the tip after the IN-09 commit (`65582a31`).

```sh
B=f328fd218dce5b9c652f8872042e922f9686b87f
X=$(git rev-parse HEAD)
D=$(git diff --name-only "$B" "$X")
git diff --quiet "$B" "$X" -- . ':(exclude).planning' ':(exclude)docs' ':(exclude)CHANGELOG.md'
git ls-tree -r --name-only "$X" | grep -x '\.planning/\.gsd-ws-arg'
git worktree add --quiet --detach /tmp/gsd16/dry "$B"
git -C /tmp/gsd16/dry merge --quiet --no-ff --no-edit "$X"
git rev-list --parents -n 1 "$M"
git diff --quiet "$M" "$X"
git worktree remove --force /tmp/gsd16/dry && git worktree prune
git rev-parse custom_build
```

```text
X=65582a31d5bdc62aa77cdf9ea873e9c2ae7b8257
allow-list diff names (top-level dirs):
 120 .planning
   2 docs
ALLOW-LIST-OK
OUTSIDE-TREE-EQUALS-B
GSD-WS-ARG-ABSENT
M=5d391866754131842695dfb2d685cd4a2ce92c76
parents-line: 5d391866754131842695dfb2d685cd4a2ce92c76 f328fd218dce5b9c652f8872042e922f9686b87f 65582a31d5bdc62aa77cdf9ea873e9c2ae7b8257
TOKENS=3
M^1=f328fd218dce5b9c652f8872042e922f9686b87f
M^2=65582a31d5bdc62aa77cdf9ea873e9c2ae7b8257
TREE(M)==TREE(X)
custom_build=f328fd218dce5b9c652f8872042e922f9686b87f
GATE-DRY-RUN-OK
```

Matches text: n/a (gate check)

### E-05: item 6 self-check grep against a scratch config set

Backs: `docs/ROLLOUT-v1.7.0_gg1.md`, item 6 "Am I affected?", the `grep -rniE` line, and the sentence below it: "a connector with `compress=true` in `jdbcConnectionProperties` and `setCompressionMethod=false` is at risk". The command is the line read verbatim from the doc, with `<connector-config-directory>` replaced by the scratch directory. The scratch set holds `pos.json` (the four risky lines: `compress=true`, `setCompressionMethod` false, `set_compression_method` false in two quoting styles) and `neg.json` (the four `true` / `compress=false` lines).

```sh
grep -rniE 'compress=true|setCompressionMethod"?[[:space:]]*[:=][[:space:]]*"?false|set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false' /tmp/gsd16/item6/cfg
```

```text
/tmp/gsd16/item6/cfg/pos.json:1:"set_compression_method": "false",
/tmp/gsd16/item6/cfg/pos.json:2:set_compression_method=false
/tmp/gsd16/item6/cfg/pos.json:3:"setCompressionMethod": false,
/tmp/gsd16/item6/cfg/pos.json:4:"jdbcConnectionProperties": "?compress=true",
```

Result: 4 positive matches (all from `pos.json`), 0 from `neg.json`. Sorted match sets are compared, because `grep -r` traversal order is unspecified.

Empty-result reading (a set holding only `true` and `compress=false` values), same pattern, `neg.json` alone in `/tmp/gsd16/item6/cfg-neg`:

```sh
grep -rniE 'compress=true|setCompressionMethod"?[[:space:]]*[:=][[:space:]]*"?false|set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false' /tmp/gsd16/item6/cfg-neg; echo "exit=$?"
```

```text
exit=1
```

Matches text: yes

Old text (base `505e4981`), same scratch set. This backs the defect IN-10 describes: the bare `set_compression_method` alternative matched the `true` lines too.

```sh
git show 505e49819fba7b2ba980a718d9ec6b66b55027b0:docs/ROLLOUT-v1.7.0_gg1.md | grep '^grep -rniE .compress=true|' | sed 's#<connector-config-directory>#/tmp/gsd16/item6/cfg#' > /tmp/gsd16/old-cmd.txt
sh /tmp/gsd16/old-cmd.txt | sort
```

```text
/tmp/gsd16/item6/cfg/neg.json:1:"set_compression_method": "true",
/tmp/gsd16/item6/cfg/neg.json:2:set_compression_method=true
/tmp/gsd16/item6/cfg/pos.json:1:"set_compression_method": "false",
/tmp/gsd16/item6/cfg/pos.json:2:set_compression_method=false
/tmp/gsd16/item6/cfg/pos.json:3:"setCompressionMethod": false,
/tmp/gsd16/item6/cfg/pos.json:4:"jdbcConnectionProperties": "?compress=true",
```

Old-text verdict: n/a (base text, replaced). Two false positives (`neg.json` lines 1 and 2) that the new pattern does not produce.

### E-06: revert-of-revert on the real release merge

Backs: `docs/ROLLOUT-v1.7.0_gg1.md`, item 8, the paragraph "To go forward again after `git revert -m 1`, revert the revert commit first (`git revert <revert-commit>`); merging `sync/v1.7.0` again without that brings nothing back." Run in a detached throwaway worktree on the release merge `f328fd21`; its second parent `f328fd21^2` is what `sync/v1.7.0` was merged from. The `-c user.name/user.email` options only give the throwaway commits an identity.

```sh
G1=f328fd218dce5b9c652f8872042e922f9686b87f
P1=c61d90ce5dfb32ea5876dffacc8bb54d8846739a
git worktree add --quiet --detach /tmp/gsd16/revert-demo "$G1"
git -C /tmp/gsd16/revert-demo -c user.name=demo -c user.email=demo@example.invalid revert --no-edit -m 1 HEAD
git -C /tmp/gsd16/revert-demo diff --quiet HEAD "$P1"
git -C /tmp/gsd16/revert-demo -c user.name=demo -c user.email=demo@example.invalid merge --no-edit "$G1^2"
git -C /tmp/gsd16/revert-demo diff --quiet HEAD "$P1"
git -C /tmp/gsd16/revert-demo -c user.name=demo -c user.email=demo@example.invalid revert --no-edit "$R"
git -C /tmp/gsd16/revert-demo diff --quiet HEAD "$G1"
git worktree remove --force /tmp/gsd16/revert-demo && git worktree prune
```

```text
R=27a6b75d232411bb74946d34a2d89e110e58c461
after revert: tree(HEAD)==tree(pre-v1.7.0-sync c61d90ce)
merge again: Already up to date.
HEAD unchanged after merge again
tree still == c61d90ce
after revert-of-revert: tree(HEAD)==tree(f328fd21)
REVERT-OF-REVERT-DEMONSTRATED
```

`R` is the revert commit. Merging the second parent again changed nothing ("Already up to date."), and reverting `R` restored the release tree.

Matches text: yes

### E-07: plain-language, ledger-tag, anchor and link check

Backs: `docs/ROLLOUT-v1.7.0_gg1.md`: the preface ledger link, the item 2 and item 5 headings and TOC entries, and every `(ledger L-nn)` tag. The check: no bare planning ID outside the verbatim startup-WARN quote line; every `L-nn` inside a `(ledger ...)` tag; every TOC anchor equals the GitHub slug of a heading; every relative link resolves; the TOC order equals the `## Item N:` heading order; eight items; exactly one WARN-quote line; the ledger link present.

```sh
python3 -I -c 'import re,os;p="docs/ROLLOUT-v1.7.0_gg1.md";t=open(p,encoding="utf-8").read();L=t.split("\n");W={i for i,l in enumerate(L) if l.startswith("Column <db>.<table>.<column> has type")};ids=[(i+1,m.group(0)) for i,l in enumerate(L) if i not in W for m in re.finditer(r"\b(?:(?!L-)[A-Z]{1,6}-\d{2}|\d{2}-REVIEW|999\.1|Phase 1\d)\b",l)];S=[m.span() for m in re.finditer(r"\(ledger [^()]*\)",t)];un=[(t.count("\n",0,m.start())+1,m.group(0)) for m in re.finditer(r"\bL-\d\d\b",t) if not any(a<=m.start()<b for a,b in S)];sl=lambda h:re.sub(r"[^\w\- ]","",h.strip().lower()).replace(" ","-");H=[sl(l.lstrip("#")) for l in L if re.match(r"#{1,6} ",l)];A=[a for a in re.findall(r"\]\(#([^)]+)\)",t) if a not in H];F=[f for f in re.findall(r"\]\(([^)#:]+)(?:#[^)]*)?\)",t) if not os.path.exists(os.path.normpath(os.path.join(os.path.dirname(p),f)))];toc=[a for a in re.findall(r"^\d\. \[[^]]+\]\(#(item-\d-[^)]+)\)",t,re.M)];hd=[sl(l.lstrip("#")) for l in L if l.startswith("## Item ")];lk="13-BEHAVIOUR-LEDGER.md" in t;print("bare-ids",ids,"untagged-L",un,"bad-anchors",A,"bad-links",F,"toc-order",toc==hd,"items",len(hd),"warn-quote-lines",len(W),"ledger-link",lk);raise SystemExit(0 if not ids and not un and not A and not F and toc==hd and len(hd)==8 and len(W)==1 and lk else 1)' && echo ROLLOUT-PLAIN-LANGUAGE-OK
git grep -n -e 'item-2-r-01' -e 'item-5-type-08' -- . ':(exclude).planning'; echo "exit=$?"
```

```text
bare-ids [] untagged-L [] bad-anchors [] bad-links [] toc-order True items 8 warn-quote-lines 1 ledger-link True
ROLLOUT-PLAIN-LANGUAGE-OK
exit=1
```

The `git grep` exit 1 means no file outside `.planning/` still links the old anchors `#item-2-r-01-behaviour-change` and `#item-5-type-08-and-arraytuple`.

Matches text: yes

### E-08: SQL blocks unchanged (7 blocks)

Backs: `docs/ROLLOUT-v1.7.0_gg1.md`, all seven `sql` blocks. The text of every SQL statement equals the base `505e4981`, so the 15-REVIEW live verification on ClickHouse 26.9.14.10 stands and the SQL is not re-run in this plan (D-07).

```sh
git show 505e49819fba7b2ba980a718d9ec6b66b55027b0:docs/ROLLOUT-v1.7.0_gg1.md > /tmp/gsd16/rollout-base.md
python3 -I -c 'import re,sys,glob;f=lambda p:re.findall(r"```sql\n(.*?)```",open(p,encoding="utf-8").read(),re.S);a=f(sys.argv[1]);b=f(sys.argv[2]);F=sorted(glob.glob("docs/*.md"))+["CHANGELOG.md","README.md"];H=[(x,n,l) for x in F for n,l in enumerate(open(x,encoding="utf-8"),1) if re.search("truncat",l,re.I)];B=[(x,n) for x,n,l in H if re.search(r"truncate\s+table",l,re.I) or not ("Do not `TRUNCATE` the table" in l or "truncate event" in l)];print("sql-blocks",len(a),len(b),"equal",a==b,"truncate-lines",[(x,n) for x,n,_ in H],"bad",B);raise SystemExit(0 if a and a==b and H and not B else 1)' /tmp/gsd16/rollout-base.md docs/ROLLOUT-v1.7.0_gg1.md && echo SQL-UNCHANGED-NO-TRUNCATE-OK
```

```text
sql-blocks 7 7 equal True truncate-lines [('docs/FORK-DELTA.md', 126), ('docs/ROLLOUT-v1.7.0_gg1.md', 111)] bad []
SQL-UNCHANGED-NO-TRUNCATE-OK
```

Matches text: yes

### E-09: no instruction to TRUNCATE the shared state table

Backs: `docs/ROLLOUT-v1.7.0_gg1.md` line 111 (the "Do not `TRUNCATE` the table." paragraph, byte-identical to `505e4981`) and the absence of any TRUNCATE instruction in `docs/*.md`, `CHANGELOG.md` and `README.md`. The `bad []` result in E-08 is the machine check; this entry lists the two lines it found.

```sh
grep -in 'truncat' docs/*.md CHANGELOG.md README.md | cut -c1-200
```

```text
docs/FORK-DELTA.md:126:**2. 13-REVIEW WR-01: the empty-CDC skip exists only on the metadata-free route.** With `bypassRowBinary=true` and no explicit `insertFormat=json`, a Debezium truncate event or 
docs/ROLLOUT-v1.7.0_gg1.md:111:Do not `TRUNCATE` the table. It is shared by every `exactlyOnce` connector with the same `zkDatabase` and `zkPath` (item 1), so truncating it also erases their state. A
```

The first line is the Debezium truncate event (a record type, not an instruction). The second is the intended negative.

Matches text: yes

### E-10: ledger link target is tracked

Backs: `docs/ROLLOUT-v1.7.0_gg1.md` preface, the link `../.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`. A tracked file at that path is part of the tree that the publication merge carries.

```sh
git ls-files --error-unmatch .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md; echo "exit=$?"
```

```text
.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
exit=0
```

Matches text: yes

### E-11: item 2 and item 8 findings check

Backs: `docs/ROLLOUT-v1.7.0_gg1.md` item 2 (IN-07, D-04) and item 8 (IN-08, D-05). The replayed-ranges sentence occurs once; item 2 opens its What to do with the exactlyOnce qualifier and ends on the nothing-to-recover sentence; the Do-not-TRUNCATE paragraph of `505e4981` is still a line of the doc; item 8 holds the revert-of-revert note.

```sh
python3 -I -c 'import subprocess as s;t=open("docs/ROLLOUT-v1.7.0_gg1.md",encoding="utf-8").read();b=s.run(["git","show","505e49819fba7b2ba980a718d9ec6b66b55027b0:docs/ROLLOUT-v1.7.0_gg1.md"],capture_output=True,check=True,encoding="utf-8").stdout;sec=lambda x,n:x[x.index("\n## Item %d:"%n):x.index("\n## Item %d:"%(n+1))] if n<8 else x[x.index("\n## Item 8:"):];i2=sec(t,2);i8=sec(t,8);dnt=[l for l in b.split("\n") if l.startswith("Do not `TRUNCATE` the table.")];c=[i2.count("replayed ranges as already stored"),"Step 1 always; step 2 only for connectors with `exactlyOnce=true`" in i2,i2.rstrip().rstrip("-").rstrip().endswith("If no connector combines an explicit `insertFormat=json` with schema\x27d records, there is nothing to recover."),len(dnt)==1 and dnt[0] in t.split("\n"),"revert the revert commit first" in i8 and "brings nothing back" in i8];print("dup-count,opener,closer,do-not-truncate-intact,item8-note",c);raise SystemExit(0 if c==[1,True,True,True,True] else 1)' && echo ROLLOUT-FINDINGS-OK
```

```text
dup-count,opener,closer,do-not-truncate-intact,item8-note [1, True, True, True, True]
ROLLOUT-FINDINGS-OK
```

Matches text: yes

## Finding map
