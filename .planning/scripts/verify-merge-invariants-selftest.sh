#!/usr/bin/env bash
#
# Fail-first proof for verify-merge-invariants.sh: each check must be able to fail.
#
# usage: verify-merge-invariants-selftest.sh [merge-sha]
#   merge-sha defaults to the oldest first-parent merge in pre-v1.7.0-sync..HEAD (the v1.7.0 sync merge 54e63648; the DEPS-01 merge of origin/custom_build is a later one).
#
# Only dangling objects are created (temporary index + commit-tree). No ref is created or moved.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

SCRIPT="${ROOT_DIR}/.planning/scripts/verify-merge-invariants.sh"
ANCHORS_PATH=".planning/scripts/fork-anchors.txt"
EXEMPTIONS_PATH=".planning/scripts/fork-anchor-exemptions.txt"
DEPENDABOT_SLF4J="6473e24f372a8b37dfd8cc8cf3124e6cff52edca"
V160_MERGE="673d29c3d5dc24b97de3b022acdfe473e1a5493c"
V160_ENV=(PRE_TAG=pre-v1.6.0-sync UPSTREAM_REF=refs/tags/v1.6.0 ANCHORS_FILE= CONFLICTED_PATHS_FILE=)

export GIT_AUTHOR_NAME="selftest" GIT_AUTHOR_EMAIL="selftest@example.invalid"
export GIT_COMMITTER_NAME="selftest" GIT_COMMITTER_EMAIL="selftest@example.invalid"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

MERGE_ARG="${1:-}"
if [ -z "${MERGE_ARG}" ]; then
  # rev-list prints newest first, so tail -1 is the oldest first-parent merge.
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
  env ${env_args[@]+"${env_args[@]}"} bash "${SCRIPT}" "$@" > "${WORK_DIR}/out" 2> "${WORK_DIR}/err"
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

# ST4: one wrong anchor count in the range-end commit trips the anchor check. The awk bumps the first data
# row of fork-anchors.txt, the TARGET_TABLE_FILTER row, so the needle names it and ST4 cannot pass on an
# unrelated violation such as the exempted 6473e24f one.
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
  expect_failure ST4 "'TARGET_TABLE_FILTER' expected" -- "${MERGE_ARG}" "${st4_commit}"
fi

# exemption_row <fields...>: the arguments joined by TABs (three or five fields build the malformed rows).
exemption_row() {
  local IFS=$'\t'
  printf '%s\n' "$*"
}

# exemption_range <x-commit> <exemptions-content-file>: dangling range end y, a child of x whose tree is
# HEAD's with the exemption file replaced. pre-v1.7.0-sync..y is then {x, y}, and y satisfies its own anchors.
exemption_range() {
  git commit-tree "$(synth_tree HEAD "${EXEMPTIONS_PATH}" "$2")" -p "$1" -m "selftest exemption range"
}

# exemption_case <name> <needle> <x-commit> <ANCHOR_EXEMPTIONS_FILE value> <exemptions-content-file>
exemption_case() {
  local y
  y="$(exemption_range "$3" "$5")"
  expect_failure "$1" "$2" CONFLICTED_PATHS_FILE= "ANCHOR_EXEMPTIONS_FILE=$4" -- "${MERGE_ARG}" "${y}"
}

# ST5-ST9: the SHA- and row-pinned exemption forgives exactly one row at exactly one commit.
SLF4J_NEEDLE="ANCHOR VIOLATION: .* build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0"
KAFKA_NEEDLE="gradle/libs.versions.toml 'kafka = \"4.1.1\"' expected 1 got 0"
EXEMPTION_NEEDLE="ANCHOR EXEMPTION VIOLATION"
EXEMPTION_CASES="ST5-empty-file ST5-disabled ST6-other-row ST6-other-commit ST7-other-bld08-row ST7-exempted-row-silent ST8-short-sha ST8-uppercase-sha ST8-missing-reason ST8-empty-reason ST8-extra-field ST9-exact-row"
PRE_FULL="$(git rev-parse --verify -q 'pre-v1.7.0-sync^{commit}' 2>/dev/null || true)"
if [ -z "${MERGE_ARG}" ] || ! git cat-file -e "${MERGE_ARG}^{commit}" 2>/dev/null; then
  skip_reason="no v1.7.0 merge commit yet"
elif ! git cat-file -e "${DEPENDABOT_SLF4J}^{commit}" 2>/dev/null; then
  skip_reason="Dependabot commit ${DEPENDABOT_SLF4J} not present"
elif [ -z "${PRE_FULL}" ]; then
  skip_reason="pre-v1.7.0-sync not present"
else
  skip_reason=""
fi

if [ -n "${skip_reason}" ]; then
  for case_name in ${EXEMPTION_CASES}; do report SKIP "${case_name}" "${skip_reason}"; done
else
  x_slf4j="$(git commit-tree "${DEPENDABOT_SLF4J}^{tree}" -p "${PRE_FULL}" -m "selftest x_slf4j")"
  git show "${DEPENDABOT_SLF4J}:gradle/libs.versions.toml" | sed 's/kafka = "4.1.1"/kafka = "4.1.2"/' > "${WORK_DIR}/catalog-bumped"
  x_both_tree="$(synth_tree "${DEPENDABOT_SLF4J}" gradle/libs.versions.toml "${WORK_DIR}/catalog-bumped")"
  x_both="$(git commit-tree "${x_both_tree}" -p "${PRE_FULL}" -m "selftest x_both")"
  slf4j_pattern="slf4j-reload4j:2.0.17"
  reason="selftest"

  : > "${WORK_DIR}/ex-empty"
  exemption_row "${x_slf4j}" build.gradle.kts "${slf4j_pattern}" "${reason}" > "${WORK_DIR}/ex-exact"
  exemption_case ST5-empty-file "${SLF4J_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-empty"
  exemption_case ST5-disabled "${SLF4J_NEEDLE}" "${x_slf4j}" "" "${WORK_DIR}/ex-exact"

  exemption_row "${x_slf4j}" gradle/libs.versions.toml 'kafka = "4.1.1"' "${reason}" > "${WORK_DIR}/ex-other-row"
  exemption_case ST6-other-row "${SLF4J_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-other-row"
  exemption_row "${PRE_FULL}" build.gradle.kts "${slf4j_pattern}" "${reason}" > "${WORK_DIR}/ex-other-commit"
  exemption_case ST6-other-commit "${SLF4J_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-other-commit"

  exemption_row "${x_both}" build.gradle.kts "${slf4j_pattern}" "${reason}" > "${WORK_DIR}/ex-both"
  exemption_case ST7-other-bld08-row "${KAFKA_NEEDLE}" "${x_both}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-both"
  # The err file still holds the ST7 run: the exempted slf4j row must be absent, proving the exemption fired.
  if grep -qF "${slf4j_pattern}" "${WORK_DIR}/err"; then
    report FAIL ST7-exempted-row-silent "stderr still reports ${slf4j_pattern}: $(head -c 300 "${WORK_DIR}/err")"
  else
    report PASS ST7-exempted-row-silent
  fi

  exemption_row "${x_slf4j:0:7}" build.gradle.kts "${slf4j_pattern}" "${reason}" > "${WORK_DIR}/ex-short"
  exemption_case ST8-short-sha "${EXEMPTION_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-short"
  exemption_row "$(printf '%s' "${x_slf4j}" | tr 'a-f' 'A-F')" build.gradle.kts "${slf4j_pattern}" "${reason}" > "${WORK_DIR}/ex-upper"
  exemption_case ST8-uppercase-sha "${EXEMPTION_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-upper"
  exemption_row "${x_slf4j}" build.gradle.kts "${slf4j_pattern}" > "${WORK_DIR}/ex-no-reason"
  exemption_case ST8-missing-reason "${EXEMPTION_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-no-reason"
  exemption_row "${x_slf4j}" build.gradle.kts "${slf4j_pattern}" "" > "${WORK_DIR}/ex-empty-reason"
  exemption_case ST8-empty-reason "${EXEMPTION_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-empty-reason"
  exemption_row "${x_slf4j}" build.gradle.kts "${slf4j_pattern}" "${reason}" extra > "${WORK_DIR}/ex-extra"
  exemption_case ST8-extra-field "${EXEMPTION_NEEDLE}" "${x_slf4j}" "${EXEMPTIONS_PATH}" "${WORK_DIR}/ex-extra"

  # ST9 is the positive control: without it ST6-ST8 would pass on any unrelated failure.
  y_exact="$(exemption_range "${x_slf4j}" "${WORK_DIR}/ex-exact")"
  env CONFLICTED_PATHS_FILE= "ANCHOR_EXEMPTIONS_FILE=${EXEMPTIONS_PATH}" bash "${SCRIPT}" "${MERGE_ARG}" "${y_exact}" > "${WORK_DIR}/out" 2> "${WORK_DIR}/err"
  rc=$?
  if [ "${rc}" -eq 0 ] && tail -1 "${WORK_DIR}/out" | grep -q 'anchor_exempt=1 result=PASS'; then
    report PASS ST9-exact-row
  else
    report FAIL ST9-exact-row "exit ${rc}: $(tail -1 "${WORK_DIR}/out") $(head -c 300 "${WORK_DIR}/err")"
  fi
fi

if [ "${failures}" -ne 0 ]; then
  echo "selftest: ${failures} case(s) failed"
  exit 1
fi
echo "selftest: no failures"
exit 0
