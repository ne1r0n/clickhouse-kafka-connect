#!/usr/bin/env python3
"""Read-only structure gate for the CHANGELOG section and the FORK-DELTA numbers of Phase 38. Run it with `python3 -I`.

It never writes a file and never runs git; the plans' shell commands pass it git output through files or
stdin. It exists so the plans' verify blocks stay short and auditable.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  changelog NEW OLD --date YYYY-MM-DD [--require TOKEN ...]
      NEW is the CHANGELOG after plan 38-01 task 2, OLD the CHANGELOG at the phase start (git show into a file).
      Checks: NEW starts with `# 1.7.0_gg6, <date>`; NEW ends with OLD minus its upstream-owned `# Unreleased`
      block (so every released section is byte-identical and only that block was deleted); the new section holds
      `## Important Changes`, `## Improvements`, `## Known Issues`, `## Build and CI` in that order; the text of
      OLD's Unreleased entry is inside the new section after its Improvements heading; no `# Unreleased` heading
      is left; the section names no planning, requirement or review identifier; no line of the section without a
      URL is longer than 125 characters; every --require TOKEN occurs in the section.
  links FILE...
      every relative Markdown link target `[text](path)` or `[text](path#fragment)` of each FILE resolves to an
      existing file or directory relative to the FILE's directory (URLs and bare `#fragment` links are skipped).
  shortstat
      reads one `git diff --shortstat` line on stdin and prints `files insertions deletions` (a missing part is 0).
  numbers FORKDELTA LABEL --wider "F I D" --narrow "F I D"
      the raw-numbers table of FORKDELTA holds the row of the wider scope and the row of `src/main` for the
      endpoint LABEL with exactly these numbers.
"""
import os
import re
import sys

PLANNING_ID = re.compile(
    r"\b(BUF|CLT|TAL|CIH|ADV|RLS|SWP|BLD|DEPS|FUT|OI|UF|SYNC|PUB|CIG|ORCH|TST|CMP)-[0-9]{1,3}\b"
    r"|\bE-[0-9]{2}\b|\b(CR|BL|WR|IN)-[0-9]{2}\b|\bT-38-")
HEADINGS = ["## Important Changes", "## Improvements", "## Known Issues", "## Build and CI"]
UNRELEASED = "# Unreleased\n"
NEXT_RELEASE = "# 1.7.0, 2026-09-30\n"
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
SHORTSTAT = re.compile(r"(\d+) files? changed(?:, (\d+) insertions?\(\+\))?(?:, (\d+) deletions?\(-\))?")


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return fh.read()
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def cmd_changelog(args):
    if len(args) < 4 or args[2] != "--date":
        die("changelog NEW OLD --date YYYY-MM-DD [--require TOKEN ...]")
    new, old, date = read(args[0]), read(args[1]), args[3]
    require = args[5:] if args[4:5] == ["--require"] else []
    if not re.fullmatch(r"\d{4}-\d\d-\d\d", date):
        die("the date is YYYY-MM-DD")
    problems = []
    if UNRELEASED not in old or NEXT_RELEASE not in old:
        die("OLD has no Unreleased block followed by the 1.7.0 section")
    i, j = old.index(UNRELEASED), old.index(NEXT_RELEASE)
    block = old[i + len(UNRELEASED):j]
    rest = old[:i] + old[j:]
    if not new.startswith("# 1.7.0_gg6, %s\n" % date):
        problems.append("the file does not start with `# 1.7.0_gg6, %s`" % date)
    if not new.endswith(rest):
        problems.append("the file does not end with the old file minus the Unreleased block (a released section changed)")
        section = ""
    else:
        section = new[:len(new) - len(rest)]
    if re.search(r"^# Unreleased\s*$", new, re.M):
        problems.append("an Unreleased heading is left")
    positions = [section.find("\n" + h + "\n") for h in HEADINGS]
    if any(p < 0 for p in positions) or positions != sorted(positions):
        problems.append("the four headings %s are not all present in that order" % HEADINGS)
    start = block.find("* New `additionalRetriableErrorCodes`")
    entry = block[start:].strip() if start >= 0 else ""
    if not entry:
        problems.append("OLD's Unreleased entry was not found (the plan expects the additionalRetriableErrorCodes entry)")
    elif entry not in section:
        problems.append("OLD's Unreleased entry text is not inside the new section verbatim")
    elif positions[1] >= 0 and section.find(entry) < positions[1]:
        problems.append("OLD's Unreleased entry is before the Improvements heading")
    for number, line in enumerate(section.split("\n"), 1):
        if PLANNING_ID.search(line):
            problems.append("section line %d names a planning or review identifier" % number)
        if "http" not in line and len(line) > 125:
            problems.append("section line %d is %d characters" % (number, len(line)))
    for token in require:
        if token not in section:
            problems.append("the section does not contain %r" % token)
    for p in problems:
        print("FAIL changelog: " + p)
    print("%s changelog: section_lines=%d required=%d" % (
        "OK" if not problems else "FAIL", len(section.split("\n")) if section else 0, len(require)))
    return 0 if not problems else 1


def cmd_links(args):
    if not args:
        die("links FILE...")
    bad = checked = 0
    for name in args:
        base = os.path.dirname(os.path.abspath(name))
        for number, line in enumerate(read(name).split("\n"), 1):
            for target in LINK.findall(line):
                if re.match(r"^[a-z][a-z0-9+.-]*:", target) or target.startswith("#"):
                    continue
                path = target.split("#", 1)[0]
                checked += 1
                if not os.path.exists(os.path.join(base, path)):
                    print("BROKEN LINK %s:%d -> %s" % (name, number, target))
                    bad += 1
    print("%s links: %d relative targets checked, %d broken" % ("OK" if bad == 0 else "FAIL", checked, bad))
    return 0 if bad == 0 else 1


def cmd_shortstat(args):
    if args:
        die("shortstat takes no argument (the line is read on stdin)")
    m = SHORTSTAT.search(sys.stdin.read())
    if not m:
        die("no `N files changed` line on stdin")
    print("%s %s %s" % (m.group(1), m.group(2) or "0", m.group(3) or "0"))
    return 0


def cmd_numbers(args):
    if len(args) != 6 or args[2] != "--wider" or args[4] != "--narrow":
        die('numbers FORKDELTA LABEL --wider "F I D" --narrow "F I D"')
    text, label = read(args[0]), args[1]
    problems = []
    for scope, spec in (("src build.gradle.kts gradle .github", args[3]), ("src/main", args[5])):
        nums = spec.split()
        if len(nums) != 3 or not all(n.isdigit() for n in nums):
            die("numbers are three integers: %r" % spec)
        row = "| `%s` | %s | %s | %s | %s |" % (scope, label, nums[0], nums[1], nums[2])
        if row not in text.split("\n"):
            problems.append("missing row: " + row)
    for p in problems:
        print("FAIL numbers: " + p)
    print("%s numbers: label %r" % ("OK" if not problems else "FAIL", label))
    return 0 if not problems else 1


COMMANDS = {"changelog": cmd_changelog, "links": cmd_links, "shortstat": cmd_shortstat, "numbers": cmd_numbers}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        die("subcommand is one of " + ", ".join(sorted(COMMANDS)))
    sys.exit(COMMANDS[sys.argv[1]](sys.argv[2:]))
