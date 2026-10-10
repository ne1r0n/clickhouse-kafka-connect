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

## Finding map
