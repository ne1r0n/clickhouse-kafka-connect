#!/usr/bin/env bash
# Dry-run merge and rollback rehearsal for docs/ROLLOUT-v1.7.0_gg8.md.
#
# Usage, from the repository root:
#   bash 49-rehearse.sh merge <TIP> <WORKDIR> [<MESSAGE_FILE>]
#   bash 49-rehearse.sh at <COMMIT> <WORKDIR>
# Environment: MODE=attr (default) or MODE=plain.
#
# It prints one key=value line per fact and ends with `rehearsal: PASS` (exit 0) or
# `rehearsal: FAIL <keys>` (exit 1); a usage error prints `USAGE ERROR: ...` and exits 2.
# It never pushes, fetches or tags, and never touches the main worktree's HEAD, index or files.
set -uo pipefail

GG7=9483476322e0d331e81c5b92ac266a18f0f01450
BRANCH=rollback-v1.7.0_gg8
NOTE_PATH=docs/ROLLOUT-v1.7.0_gg8.md
ALLOWLIST_PATH=.planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt
NOTE_CHECK_REL=.planning/phases/49-release-publication/49-note-check.py

usage_error() {
  printf 'USAGE ERROR: %s\n' "$1"
  exit 2
}

join_lines() {
  local joined
  joined=$(printf '%s\n' "$1" | paste -sd '|' - | sed 's/|/; /g')
  if [ -z "${joined}" ]; then printf 'clean'; else printf '%s' "${joined}"; fi
}

[ "$#" -ge 3 ] || usage_error "bash 49-rehearse.sh merge <TIP> <WORKDIR> [<MESSAGE_FILE>] | at <COMMIT> <WORKDIR>"
SUB=$1
ARG=$2
WORKDIR=$3
MSGFILE=${4:-}
case "${SUB}" in
  merge) [ "$#" -le 4 ] || usage_error "merge takes <TIP> <WORKDIR> [<MESSAGE_FILE>]" ;;
  at) [ "$#" -eq 3 ] || usage_error "at takes <COMMIT> <WORKDIR>" ;;
  *) usage_error "the first argument is merge or at" ;;
esac
MODE=${MODE:-attr}
case "${MODE}" in attr | plain) ;; *) usage_error "MODE is attr or plain" ;; esac

MAIN_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || usage_error "run it inside the repository"
[ ! -e "${WORKDIR}" ] || usage_error "${WORKDIR} already exists"
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  usage_error "branch ${BRANCH} already exists"
fi
PRE=$(git rev-parse --verify 'pre-v1.7.0_gg8^{commit}' 2>/dev/null) || usage_error "pre-v1.7.0_gg8 does not resolve to a commit"
TARGET=$(git rev-parse --verify "${ARG}^{commit}" 2>/dev/null) || usage_error "${ARG} does not resolve to a commit"
if [ -n "${MSGFILE}" ]; then
  case "${MSGFILE}" in /*) ;; *) MSGFILE="$(pwd)/${MSGFILE}" ;; esac
  [ -f "${MSGFILE}" ] || usage_error "message file ${MSGFILE} is not readable"
fi
case "${WORKDIR}" in /*) ;; *) WORKDIR="$(pwd)/${WORKDIR}" ;; esac

export GIT_AUTHOR_NAME=rehearsal GIT_COMMITTER_NAME=rehearsal
export GIT_AUTHOR_EMAIL=rehearsal@example.invalid GIT_COMMITTER_EMAIL=rehearsal@example.invalid
ATTR=""
if [ "${MODE}" = attr ]; then
  EMPTY_TREE=$(git hash-object -t tree /dev/null)
  ATTR="--attr-source=${EMPTY_TREE}"
fi
NOTE_CHECK="${MAIN_ROOT}/${NOTE_CHECK_REL}"
WT="${WORKDIR}/d"

cleanup() {
  cd "${MAIN_ROOT}" 2>/dev/null || true
  if [ -d "${WT}" ]; then
    git worktree remove --force "${WT}" >/dev/null 2>&1 || true
  fi
  git worktree prune >/dev/null 2>&1 || true
  if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git branch -D "${BRANCH}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

FAILED=""
fact() {
  printf '%s=%s\n' "$1" "$2"
}
need() {
  # need KEY ACTUAL EXPECTED
  if [ "$2" != "$3" ]; then FAILED="${FAILED}${FAILED:+ }$1"; fi
}

mkdir -p "${WORKDIR}" || usage_error "cannot create ${WORKDIR}"
fact mode "${MODE}"

if [ "${SUB}" = merge ]; then
  git worktree add --detach "${WT}" "${GG7}" >/dev/null 2>&1 || { fact worktree_add failed; printf 'rehearsal: FAIL worktree_add\n'; exit 1; }
  cd "${WT}" || exit 1
  if [ -n "${MSGFILE}" ]; then
    git ${ATTR} merge --no-ff -F "${MSGFILE}" "${TARGET}" >"${WORKDIR}/merge.out" 2>&1
  else
    git ${ATTR} merge --no-ff -m "Dry-run merge of ${TARGET} onto v1.7.0_gg7" "${TARGET}" >"${WORKDIR}/merge.out" 2>&1
  fi
  MERGE_EXIT=$?
  START=$(git rev-parse HEAD)
  PARENTS=$(git rev-list --parents -n 1 HEAD | cut -d' ' -f2- )
  if git diff --quiet HEAD "${TARGET}"; then TREE_EQ=yes; else TREE_EQ=no; fi
  fact start_commit "${START}"
  fact merge_exit "${MERGE_EXIT}"
  fact d_commit "${START}"
  fact d_parents "${PARENTS}"
  fact d_tree_equal "${TREE_EQ}"
  fact d_subject "$(git log -1 --format=%s HEAD)"
  need merge_exit "${MERGE_EXIT}" 0
  need d_tree_equal "${TREE_EQ}" yes
  need d_parents "${PARENTS}" "${GG7} ${TARGET}"
else
  git worktree add --detach "${WT}" "${TARGET}" >/dev/null 2>&1 || { fact worktree_add failed; printf 'rehearsal: FAIL worktree_add\n'; exit 1; }
  cd "${WT}" || exit 1
  START=${TARGET}
  fact start_commit "${START}"
fi

D_VERSION=$(tr -d '[:space:]' <VERSION)
if [ -e .gsd-ws-arg ]; then NO_WS=no; else NO_WS=yes; fi
if [ -e "${ALLOWLIST_PATH}" ]; then ALLOW=yes; else ALLOW=no; fi
TAGS=$(git tag --merged HEAD --list 'v*')
BASELINE=$(printf '%s\n' "${TAGS}" | python3 -I .github/scripts/baseline_tag.py "${D_VERSION}" 2>/dev/null)
STATUS_BEFORE=$(join_lines "$(git status --porcelain)")
fact d_version "${D_VERSION}"
fact d_no_gsd_ws_arg "${NO_WS}"
fact d_allowlist_present "${ALLOW}"
fact d_baseline_tag "${BASELINE}"
fact d_status_before_block "${STATUS_BEFORE}"
need d_version "${D_VERSION}" v1.7.0_gg8
need d_baseline_tag "${BASELINE}" v1.7.0_gg7

python3 -I "${NOTE_CHECK}" rollback "${NOTE_PATH}" --print >"${WORKDIR}/block.sh" 2>"${WORKDIR}/block.err"
EXTRACT_EXIT=$?
if [ "${EXTRACT_EXIT}" -eq 0 ]; then EXTRACT=ok; else EXTRACT="exit-${EXTRACT_EXIT}"; fi
BLOCK_SHA=$(shasum -a 256 "${WORKDIR}/block.sh" | cut -d' ' -f1)
fact block_extract "${EXTRACT}"
fact block_sha256 "${BLOCK_SHA}"
need block_extract "${EXTRACT}" ok

ROLLBACK_OUT=$(bash -e "${WORKDIR}/block.sh" 2>"${WORKDIR}/block-run.err")
ROLLBACK_EXIT=$?
ROLLBACK_LAST=$(printf '%s\n' "${ROLLBACK_OUT}" | tail -n 1)
ROLLBACK_COMMIT=$(git rev-parse HEAD)
STATUS_AFTER_RAW=$(git status --porcelain)
STATUS_AFTER=$(join_lines "${STATUS_AFTER_RAW}")
if printf '%s\n' "${STATUS_AFTER_RAW}" | grep -q 'gradlew\.bat'; then BAT=yes; else BAT=no; fi
R_VERSION=$(tr -d '[:space:]' <VERSION)
if [ -e "${NOTE_PATH}" ]; then NOTE_ABSENT=no; else NOTE_ABSENT=yes; fi
if git diff --quiet HEAD pre-v1.7.0_gg8; then TREE_TAG=yes; else TREE_TAG=no; fi
fact rollback_exit "${ROLLBACK_EXIT}"
fact rollback_output "${ROLLBACK_LAST}"
fact rollback_commit "${ROLLBACK_COMMIT}"
fact rollback_status_after "${STATUS_AFTER}"
fact gradlew_bat_trap "${BAT}"
fact rollback_version "${R_VERSION}"
fact rollback_note_absent "${NOTE_ABSENT}"
fact rollback_tree_equals_tag "${TREE_TAG}"
need rollback_exit "${ROLLBACK_EXIT}" 0
need rollback_output "${ROLLBACK_LAST}" ok
need rollback_version "${R_VERSION}" v1.7.0_gg7
need rollback_note_absent "${NOTE_ABSENT}" yes
need rollback_tree_equals_tag "${TREE_TAG}" yes

git ${ATTR} revert --no-edit "${ROLLBACK_COMMIT}" >"${WORKDIR}/revert.out" 2>&1
REVERT_EXIT=$?
if git diff --quiet HEAD "${START}"; then REVERT_TREE=yes; else REVERT_TREE=no; fi
fact revert_exit "${REVERT_EXIT}"
fact revert_restores_tree "${REVERT_TREE}"
need revert_exit "${REVERT_EXIT}" 0
need revert_restores_tree "${REVERT_TREE}" yes

cd "${MAIN_ROOT}" || exit 1
git worktree remove --force "${WT}" >/dev/null 2>&1
git worktree prune >/dev/null 2>&1
git branch -D "${BRANCH}" >/dev/null 2>&1
if [ ! -e "${WT}" ] && ! git show-ref --verify --quiet "refs/heads/${BRANCH}"; then CLEANED=yes; else CLEANED=no; fi
fact rehearsal_cleaned "${CLEANED}"
need rehearsal_cleaned "${CLEANED}" yes

if [ -z "${FAILED}" ]; then
  printf 'rehearsal: PASS\n'
  exit 0
fi
printf 'rehearsal: FAIL %s\n' "${FAILED}"
exit 1
