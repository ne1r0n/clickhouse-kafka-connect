#!/usr/bin/env bash
#
# Fail-first proof for verify-merge-invariants.sh: each check must be able to fail.
#
# usage: verify-merge-invariants-selftest.sh [merge-sha]
#   merge-sha defaults to the single first-parent merge in pre-v1.7.0-sync..HEAD.
#
# Only dangling objects are created (temporary index + commit-tree). No ref is created or moved.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

SCRIPT="${ROOT_DIR}/.planning/scripts/verify-merge-invariants.sh"
ANCHORS_PATH=".planning/scripts/fork-anchors.txt"
V160_MERGE="673d29c3d5dc24b97de3b022acdfe473e1a5493c"
V160_ENV=(PRE_TAG=pre-v1.6.0-sync UPSTREAM_REF=refs/tags/v1.6.0 ANCHORS_FILE= CONFLICTED_PATHS_FILE=)

export GIT_AUTHOR_NAME="selftest" GIT_AUTHOR_EMAIL="selftest@example.invalid"
export GIT_COMMITTER_NAME="selftest" GIT_COMMITTER_EMAIL="selftest@example.invalid"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

MERGE_ARG="${1:-}"
if [ -z "${MERGE_ARG}" ]; then
  MERGE_ARG="$(git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD | tail -1)"
fi

failures=0

report() { # status name [detail]
  case "$1" in
    PASS) echo "PASS $2" ;;
    SKIP) echo "SKIP $2 ($3)" ;;
    FAIL) echo "FAIL $2${3:+ ($3)}"; failures=$((failures + 1)) ;;
  esac
}

# synth_tree <base-commit> <path> <content-file>: dangling tree = base tree with <path> replaced.
synth_tree() {
  local base="$1" path="$2" content="$3" index blob mode
  index="${WORK_DIR}/index-$RANDOM$RANDOM"
  GIT_INDEX_FILE="${index}" git read-tree "${base}"
  mode="$(git ls-tree "${base}" -- "${path}" | awk '{print $1}')"
  blob="$(git hash-object -w "${content}")"
  GIT_INDEX_FILE="${index}" git update-index --add --cacheinfo "${mode:-100644},${blob},${path}"
  GIT_INDEX_FILE="${index}" git write-tree
}

# expect_failure <name> <needle> <env...> -- <script-args...>
expect_failure() {
  local name="$1" needle="$2"; shift 2
  local -a env_args=()
  while [ "$1" != "--" ]; do env_args+=("$1"); shift; done
  shift
  env "${env_args[@]}" bash "${SCRIPT}" "$@" > "${WORK_DIR}/out" 2> "${WORK_DIR}/err"
  local rc=$?
  if [ "${rc}" -ne 0 ] && grep -q "${needle}" "${WORK_DIR}/err"; then
    report PASS "${name}"
  else
    report FAIL "${name}" "exit ${rc}, wanted non-zero with '${needle}' on stderr: $(head -c 300 "${WORK_DIR}/err")"
  fi
}

# ST1: the v1.6.0 merge still passes with the v1.6.0 parameter values.
if ! git cat-file -e "${V160_MERGE}^{commit}" 2>/dev/null; then
  report SKIP ST1 "v1.6.0 merge ${V160_MERGE} not present"
else
  env "${V160_ENV[@]}" bash "${SCRIPT}" "${V160_MERGE}" pre-v1.7.0-sync > "${WORK_DIR}/out" 2> "${WORK_DIR}/err"
  rc=$?
  if [ "${rc}" -eq 0 ] && tail -1 "${WORK_DIR}/out" | grep -q 'result=PASS'; then
    report PASS ST1
  else
    report FAIL ST1 "exit ${rc}: $(head -c 300 "${WORK_DIR}/err")"
  fi
fi

# ST2: one authored line appended to a merged file trips line provenance.
if ! git cat-file -e "${V160_MERGE}^{commit}" 2>/dev/null; then
  report SKIP ST2 "v1.6.0 merge ${V160_MERGE} not present"
else
  { git show "${V160_MERGE}:VERSION"; echo "selftest-authored-line"; } > "${WORK_DIR}/version-authored"
  st2_tree="$(synth_tree "${V160_MERGE}" VERSION "${WORK_DIR}/version-authored")"
  st2_commit="$(git commit-tree "${st2_tree}" -p "${V160_MERGE}^1" -p "${V160_MERGE}^2" -m "selftest ST2")"
  expect_failure ST2 'PROVENANCE VIOLATION' "${V160_ENV[@]}" -- "${st2_commit}" pre-v1.7.0-sync
fi

# ST3: a merge whose second parent is not UPSTREAM_REF trips the shape check.
if [ -z "${MERGE_ARG}" ] || ! git cat-file -e "${MERGE_ARG}^{commit}" 2>/dev/null; then
  report SKIP ST3 "no v1.7.0 merge commit yet"
else
  st3_commit="$(git commit-tree "${MERGE_ARG}^{tree}" -p "${MERGE_ARG}^1" -p refs/tags/v1.6.0 -m "selftest ST3")"
  expect_failure ST3 'INVARIANT 0 VIOLATION' ANCHORS_FILE= CONFLICTED_PATHS_FILE= -- "${st3_commit}" HEAD
fi

# ST4: one wrong anchor count in the range-end commit trips the anchor check.
if [ -z "${MERGE_ARG}" ] || ! git cat-file -e "${MERGE_ARG}^{commit}" 2>/dev/null; then
  report SKIP ST4 "no v1.7.0 merge commit yet"
elif ! git cat-file -e "HEAD:${ANCHORS_PATH}" 2>/dev/null; then
  report SKIP ST4 "${ANCHORS_PATH} not committed at HEAD yet"
else
  git show "HEAD:${ANCHORS_PATH}" \
    | awk -F'\t' -v OFS='\t' '!done && $0 !~ /^#/ && NF { $3 = $3 + 1; done = 1 } { print }' \
    > "${WORK_DIR}/anchors-wrong"
  st4_tree="$(synth_tree HEAD "${ANCHORS_PATH}" "${WORK_DIR}/anchors-wrong")"
  st4_commit="$(git commit-tree "${st4_tree}" -p HEAD -m "selftest ST4")"
  expect_failure ST4 'ANCHOR VIOLATION' -- "${MERGE_ARG}" "${st4_commit}"
fi

if [ "${failures}" -ne 0 ]; then
  echo "selftest: ${failures} case(s) failed"
  exit 1
fi
echo "selftest: no failures"
exit 0
