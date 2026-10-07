#!/usr/bin/env bash

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MERGE_SHA="${1:?usage: verify-merge-invariants.sh <merge-commit-sha> [range-end]}"
RANGE_END="${2:-HEAD}"

PRE_TAG="pre-v1.6.0-sync"
UPSTREAM_REF="v1.6.0"
CONFLICTED_PATHS_FILE="${ROOT_DIR}/.planning/scripts/conflicted-paths.txt"

fail=0

# Invariant 1 (D-02): every conflicted path is byte-identical to upstream at the merge commit.
if [ ! -f "${CONFLICTED_PATHS_FILE}" ] || [ ! -s "${CONFLICTED_PATHS_FILE}" ]; then
  echo "INVARIANT 1 VIOLATION: conflicted-paths file '${CONFLICTED_PATHS_FILE}' is missing or empty" >&2
  fail=1
else
  while IFS= read -r path; do
    [ -z "${path}" ] && continue
    if ! git diff --quiet "${UPSTREAM_REF}" "${MERGE_SHA}" -- "${path}"; then
      echo "INVARIANT 1 VIOLATION: ${path} differs from ${UPSTREAM_REF} at ${MERGE_SHA}" >&2
      fail=1
    fi
  done < "${CONFLICTED_PATHS_FILE}"
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
fi

exit "${fail}"
