#!/usr/bin/env bash
#
# Merge-invariant gate for an upstream sync (parameterised, D-09).
#
# usage: verify-merge-invariants.sh <merge-commit-sha> [range-end]
#
# Environment overrides (an explicitly empty value stays empty and, where noted, skips that check):
#   PRE_TAG                 rollback tag naming the pre-sync fork tip   (default pre-v1.7.0-sync)
#   UPSTREAM_REF            full ref of the upstream release merged     (default refs/tags/v1.7.0)
#   CONFLICTED_PATHS_FILE   repo-relative record of the conflicted paths, read from RANGE_END's tree;
#                           empty skips the cross-check                  (default .planning/scripts/conflicted-paths-v1.7.0.txt)
#   ANCHORS_FILE            repo-relative fork anchor inventory, read from every commit's tree;
#                           empty skips invariant 3                      (default .planning/scripts/fork-anchors.txt)

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MERGE_SHA="${1:?usage: verify-merge-invariants.sh <merge-commit-sha> [range-end]}"
RANGE_END="${2:-HEAD}"

PRE_TAG="${PRE_TAG-pre-v1.7.0-sync}"
UPSTREAM_REF="${UPSTREAM_REF-refs/tags/v1.7.0}"
CONFLICTED_PATHS_FILE="${CONFLICTED_PATHS_FILE-.planning/scripts/conflicted-paths-v1.7.0.txt}"
ANCHORS_FILE="${ANCHORS_FILE-.planning/scripts/fork-anchors.txt}"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

TAB="$(printf '\t')"
fail=0
provenance_paths=0
anchor_commits=0
anchor_rows=0
marker_commits=0

# Invariant 0 (shape): the merge has exactly two parents, the second is the upstream release, and the
# rollback tag is an ancestor of the first parent. Provenance below is meaningless without this shape.
shape_ok=0
MERGE_FULL="$(git rev-parse --verify -q "${MERGE_SHA}^{commit}" 2>/dev/null || true)"
if [ -z "${MERGE_FULL}" ]; then
  echo "INVARIANT 0 VIOLATION: '${MERGE_SHA}' does not resolve to a commit" >&2
  fail=1
else
  read -ra merge_line <<< "$(git rev-list --parents -n 1 "${MERGE_FULL}")"
  parent_count=$(( ${#merge_line[@]} - 1 ))
  UPSTREAM_COMMIT="$(git rev-parse --verify -q "${UPSTREAM_REF}^{commit}" 2>/dev/null || true)"
  PRE_COMMIT="$(git rev-parse --verify -q "${PRE_TAG}^{commit}" 2>/dev/null || true)"
  if [ "${parent_count}" -ne 2 ]; then
    echo "INVARIANT 0 VIOLATION: ${MERGE_FULL} has ${parent_count} parent(s), expected exactly 2" >&2
    fail=1
  elif [ -z "${UPSTREAM_COMMIT}" ]; then
    echo "INVARIANT 0 VIOLATION: UPSTREAM_REF '${UPSTREAM_REF}' does not resolve to a commit" >&2
    fail=1
  elif [ "${merge_line[2]}" != "${UPSTREAM_COMMIT}" ]; then
    echo "INVARIANT 0 VIOLATION: second parent ${merge_line[2]} is not ${UPSTREAM_REF} (${UPSTREAM_COMMIT})" >&2
    fail=1
  elif [ -z "${PRE_COMMIT}" ]; then
    echo "INVARIANT 0 VIOLATION: PRE_TAG '${PRE_TAG}' does not resolve to a commit" >&2
    fail=1
  else
    # The rollback tag may itself be the integration merge that published the previous sync
    # (custom_build's --no-ff merge commit). The scratch branch then descends from that merge's
    # parents rather than from the merge, so accept a tag whose every parent is an ancestor.
    pre_contained=1
    if ! git merge-base --is-ancestor "${PRE_COMMIT}" "${merge_line[1]}"; then
      pre_contained=0
      read -ra pre_line <<< "$(git rev-list --parents -n 1 "${PRE_COMMIT}")"
      if [ "${#pre_line[@]}" -gt 2 ]; then
        pre_contained=1
        for pre_parent in "${pre_line[@]:1}"; do
          git merge-base --is-ancestor "${pre_parent}" "${merge_line[1]}" || pre_contained=0
        done
      fi
    fi
    if [ "${pre_contained}" -ne 1 ]; then
      echo "INVARIANT 0 VIOLATION: ${PRE_TAG} (or, for a merge tag, each of its parents) is not an ancestor of first parent ${merge_line[1]}" >&2
      fail=1
    else
      shape_ok=1
    fi
  fi
fi

# Invariant 1 (D-02, line provenance): every line of every conflicted or changed path at the merge
# exists in the first or the second parent's copy of that path. Set membership only: line order and
# multiplicity are left to the compile + testUnit gate, because union hunks legitimately repeat
# shared trailer lines.
if [ "${shape_ok}" -eq 1 ]; then
  P1="${merge_line[1]}"
  P2="${merge_line[2]}"
  mt_out="$(git merge-tree --write-tree --name-only "${P1}" "${P2}" 2>/dev/null)"
  mt_rc=$?
  if [ "${mt_rc}" -gt 1 ]; then
    echo "INVARIANT 0 VIOLATION: git merge-tree failed with exit status ${mt_rc} on the merge parents" >&2
    fail=1
    mt_out=""
  fi
  # Line 1 is the result tree OID; the conflicted names follow until the first blank line.
  printf '%s\n' "${mt_out}" | awk 'NR==1{next} /^$/{exit} {print}' | LC_ALL=C sort -u > "${WORK_DIR}/conflicted-derived"

  { cat "${WORK_DIR}/conflicted-derived"; git -c core.quotePath=false diff --name-only "${P1}" "${MERGE_FULL}"; } \
    | sed '/^$/d' | LC_ALL=C sort -u > "${WORK_DIR}/paths"

  # fd 3, not stdin: git inside a plain `while read` loop consumes the loop's input.
  while IFS= read -r p <&3; do
    git cat-file -e "${MERGE_FULL}:${p}" 2>/dev/null || continue
    provenance_paths=$((provenance_paths + 1))
    # A path taken whole from one side is trivially authored-line free. This also keeps binary files
    # (a jar has NULs that truncate grep -f patterns) out of the line comparison.
    merged_oid="$(git rev-parse "${MERGE_FULL}:${p}")"
    if [ "${merged_oid}" = "$(git rev-parse -q --verify "${P1}:${p}" 2>/dev/null)" ] \
      || [ "${merged_oid}" = "$(git rev-parse -q --verify "${P2}:${p}" 2>/dev/null)" ]; then
      continue
    fi
    if git diff --numstat "${P1}" "${MERGE_FULL}" -- "${p}" | grep -q '^-'; then
      echo "PROVENANCE VIOLATION: binary ${p} differs from both parents" >&2
      fail=1
      continue
    fi
    : > "${WORK_DIR}/union"
    for side in "${P1}" "${P2}"; do
      if git cat-file -e "${side}:${p}" 2>/dev/null; then
        { git show "${side}:${p}"; echo; } >> "${WORK_DIR}/union"
      fi
    done
    git show "${MERGE_FULL}:${p}" > "${WORK_DIR}/merged"
    LC_ALL=C grep -a -vxFf "${WORK_DIR}/union" "${WORK_DIR}/merged" > "${WORK_DIR}/extra"
    grep_rc=$?
    if [ "${grep_rc}" -gt 1 ]; then
      echo "PROVENANCE VIOLATION: line comparison failed with exit status ${grep_rc} for ${p}" >&2
      fail=1
    else
      extra_count="$(wc -l < "${WORK_DIR}/extra" | tr -d ' ')"
      if [ "${extra_count}" != "0" ]; then
        echo "PROVENANCE VIOLATION: ${extra_count} authored line(s) in ${p}" >&2
        fail=1
      fi
    fi
  done 3< "${WORK_DIR}/paths"

  # Conflicted-paths cross-check: the committed record must equal what merge-tree derives.
  if [ -n "${CONFLICTED_PATHS_FILE}" ]; then
    if ! git show "${RANGE_END}:${CONFLICTED_PATHS_FILE}" > "${WORK_DIR}/conflicted-recorded.raw" 2>/dev/null; then
      echo "CONFLICTED-PATHS MISMATCH: ${CONFLICTED_PATHS_FILE} is missing at ${RANGE_END}" >&2
      fail=1
    else
      sed '/^[[:space:]]*$/d' "${WORK_DIR}/conflicted-recorded.raw" | LC_ALL=C sort -u > "${WORK_DIR}/conflicted-recorded"
      if ! diff -u "${WORK_DIR}/conflicted-recorded" "${WORK_DIR}/conflicted-derived" > "${WORK_DIR}/conflicted-diff"; then
        echo "CONFLICTED-PATHS MISMATCH: ${CONFLICTED_PATHS_FILE} differs from the merge-tree conflict list" >&2
        cat "${WORK_DIR}/conflicted-diff" >&2
        fail=1
      fi
    fi
  fi
fi

# Invariant 2 (SYNC-06): no revision in ${PRE_TAG}..${RANGE_END} contains a literal conflict-start
# marker in tracked source. The marker is assembled at runtime so this script's own body never
# contains the literal 7-character sequence, or it would flag the very commit that adds it.
MARKER="$(printf '%.0s<' 1 2 3 4 5 6 7)"

SOURCE_PATHSPEC=(src build.gradle.kts settings.gradle.kts gradle .github VERSION .gitignore)

RANGE_COMMITS="$(git rev-list "${PRE_TAG}..${RANGE_END}" 2>/dev/null || true)"
if [ -z "${RANGE_COMMITS}" ]; then
  echo "INVARIANT 2 VIOLATION: range ${PRE_TAG}..${RANGE_END} contains zero commits" >&2
  fail=1
else
  for c in ${RANGE_COMMITS}; do
    # Exit 0 = marker found, 1 = clean, >=2 = the search itself failed. A failed search proves
    # nothing about the commit, so it must be reported as a violation rather than read as clean.
    grep_err="$(git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}" 2>&1)"
    grep_rc=$?
    case "${grep_rc}" in
      0) violation="${MARKER} marker found in commit ${c}" ;;
      1) continue ;;
      *) violation="search failed with exit status ${grep_rc} on commit ${c}: ${grep_err}" ;;
    esac
    echo "INVARIANT 2 VIOLATION: ${violation}" >&2
    fail=1
  done
  marker_commits="$(printf '%s\n' "${RANGE_COMMITS}" | grep -c .)"
fi

# Invariant 3 (D-08): fork anchors. Every commit in the range that carries ANCHORS_FILE must satisfy
# its own copy of the file, so an anchor can only change in the commit that rewrites the file.
# Row format: path TAB fixed-string TAB expected-matching-line-count TAB backs-id
is_data_row() {
  case "$1" in
    ''|'#'*) return 1 ;;
  esac
  [ -n "${1//[[:space:]]/}" ]
}

if [ -n "${ANCHORS_FILE}" ]; then
  if ! git show "${RANGE_END}:${ANCHORS_FILE}" > "${WORK_DIR}/anchors-end" 2>/dev/null; then
    echo "ANCHOR VIOLATION: no anchors at ${RANGE_END} (${ANCHORS_FILE} is missing)" >&2
    fail=1
  else
    while IFS= read -r row <&4 || [ -n "${row}" ]; do
      is_data_row "${row}" && anchor_rows=$((anchor_rows + 1))
    done 4< "${WORK_DIR}/anchors-end"
    if [ "${anchor_rows}" -eq 0 ]; then
      echo "ANCHOR VIOLATION: no anchors at ${RANGE_END} (${ANCHORS_FILE} has zero data rows)" >&2
      fail=1
    fi
  fi

  for c in ${RANGE_COMMITS}; do
    git show "${c}:${ANCHORS_FILE}" > "${WORK_DIR}/anchors-c" 2>/dev/null || continue
    anchor_commits=$((anchor_commits + 1))
    short_c="$(git rev-parse --short "${c}")"
    while IFS= read -r row <&3 || [ -n "${row}" ]; do
      is_data_row "${row}" || continue
      tabs="${row//[^${TAB}]/}"
      if [ "${#tabs}" -ne 3 ]; then
        echo "ANCHOR VIOLATION: ${short_c} malformed row (need 4 TAB-separated fields): ${row}" >&2
        fail=1
        continue
      fi
      a_path="${row%%${TAB}*}"
      rest="${row#*${TAB}}"
      a_pattern="${rest%%${TAB}*}"
      rest="${rest#*${TAB}}"
      a_expected="${rest%%${TAB}*}"
      a_backs="${rest#*${TAB}}"
      if [ -z "${a_path}" ] || [ -z "${a_pattern}" ] || [ -z "${a_backs}" ] || ! [[ "${a_expected}" =~ ^[0-9]+$ ]]; then
        echo "ANCHOR VIOLATION: ${short_c} malformed row (empty field or non-integer count): ${row}" >&2
        fail=1
        continue
      fi
      blob_oid="$(git rev-parse --verify -q "${c}:${a_path}" 2>/dev/null || true)"
      if [ -z "${blob_oid}" ]; then
        echo "ANCHOR VIOLATION: ${short_c} ${a_path} '${a_pattern}' expected ${a_expected} got <path missing> (${a_backs})" >&2
        fail=1
        continue
      fi
      blob_file="${WORK_DIR}/blob-${blob_oid}"
      [ -f "${blob_file}" ] || git cat-file blob "${blob_oid}" > "${blob_file}"
      a_actual="$(LC_ALL=C grep -cF -e "${a_pattern}" "${blob_file}")"
      grep_rc=$?
      if [ "${grep_rc}" -gt 1 ]; then
        echo "ANCHOR VIOLATION: ${short_c} ${a_path} '${a_pattern}' search failed with exit status ${grep_rc} (${a_backs})" >&2
        fail=1
      elif [ "${a_actual}" != "${a_expected}" ]; then
        echo "ANCHOR VIOLATION: ${short_c} ${a_path} '${a_pattern}' expected ${a_expected} got ${a_actual} (${a_backs})" >&2
        fail=1
      fi
    done 3< "${WORK_DIR}/anchors-c"
  done
fi

if [ "${fail}" -eq 0 ]; then result=PASS; else result=FAIL; fi
echo "invariants: provenance_paths=${provenance_paths} anchor_commits=${anchor_commits} anchor_rows=${anchor_rows} marker_commits=${marker_commits} result=${result}"
exit "${fail}"
