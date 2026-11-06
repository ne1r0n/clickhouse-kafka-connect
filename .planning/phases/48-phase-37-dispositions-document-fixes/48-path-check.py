"""FORK-DELTA evidence-path resolver (Phase 48, DOC-02).

Usage: python3 -I 48-path-check.py REV DOC [--list]

Reads DOC at the commit or tag REV with `git show REV:DOC`, extracts every token
that names a repository path, and resolves each one against the tracked files at
REV (`git ls-tree -r --name-only REV`). It writes no file. Exit 0 when no
candidate is unresolved, 1 otherwise, 2 on a usage or git error.

Token rule: matches of [^\\s`()\\[\\],;|]+ with trailing . : ' " and leading ' "
stripped, kept when the token starts with a repository root directory or is
.gitattributes or .gitignore. A token containing < > { } is a placeholder and is
counted, not resolved. A token containing "..." is abbreviated (".../" stands
for zero or more whole directories) and resolves when exactly one tracked file
matches. A token with * or ? resolves when a tracked file matches the glob; any
other token resolves when it is a tracked file or a directory holding one.

BY_DESIGN lists the six tokens that are absent on purpose; each reason is in the
comment next to it. The list comes from the FORK-DELTA review of Phase 48 and is
not extended by this script.

Frozen after E-02 of the Phase 48 evidence: later gates run the same bytes.
"""
import collections
import fnmatch
import re
import subprocess
import sys

ROOTS = (".planning/", "src/", "docs/", ".github/", "build/", "gradle/", "scripts/", "config/")
EXACT = (".gitattributes", ".gitignore")
TOKEN_RE = re.compile(r"[^\s`()\[\],;|]+")
PLACEHOLDER_CHARS = "<>{}"
BY_DESIGN = (
    ".github/workflows/test-head.yaml",  # upstream's file the fork deleted; FORK-DELTA records the deletion
    ".planning/scripts/compare-confluent-archives.py",  # the moved-from name of the compare script
    "build/raw-name-matrix",  # a generated, uncommitted build directory
    "gradle/actions/setup-gradle",  # a GitHub action name, not a path
    "gradle/verification-metadata.xml",  # absent by decision (FORK-DELTA item 18)
    "src/test/java/transforms/KeyToValueTest.java",  # the fork's copy that was removed; the row names it as removed
)
EXIT_USAGE = 2


def git(*args):
    try:
        done = subprocess.run(["git", *args], capture_output=True, check=True)
    except (OSError, subprocess.CalledProcessError) as err:
        sys.stderr.write("git %s failed: %s\n" % (" ".join(args), err))
        sys.exit(EXIT_USAGE)
    return done.stdout.decode("utf-8")


def candidates(text):
    found = collections.defaultdict(set)
    for match in TOKEN_RE.finditer(text):
        token = match.group(0).rstrip(".:'\"").lstrip("'\"")
        if not (token.startswith(ROOTS) or token in EXACT):
            continue
        found[token].add(text.count("\n", 0, match.start()) + 1)
    return found


def main(argv):
    flags = [a for a in argv if a.startswith("--")]
    args = [a for a in argv if not a.startswith("--")]
    if len(args) != 2 or any(f != "--list" for f in flags):
        sys.stderr.write("usage: 48-path-check.py REV DOC [--list]\n")
        return EXIT_USAGE
    rev, doc = args
    text = git("show", "%s:%s" % (rev, doc))
    tracked = [p for p in git("ls-tree", "-r", "--name-only", rev).split("\n") if p]
    directories = set()
    for name in tracked:
        parts = name.split("/")
        for i in range(1, len(parts)):
            directories.add("/".join(parts[:i]))
    tracked_set = set(tracked)

    resolved = abbreviated = abbreviated_unique = placeholders = by_design = 0
    unresolved = []
    listed = []
    found = candidates(text)
    for token in sorted(found):
        stem = token.rstrip("/")
        if any(c in token for c in PLACEHOLDER_CHARS):
            placeholders += 1
        elif stem in BY_DESIGN:
            by_design += 1
            listed.append(token)
        elif "..." in stem:
            abbreviated += 1
            pattern = re.escape(stem).replace(re.escape(".../"), "(?:[^/]+/)*")
            hits = [p for p in tracked if re.fullmatch(pattern, p)]
            if len(hits) == 1:
                abbreviated_unique += 1
            else:
                unresolved.append(token)
        elif "*" in stem or "?" in stem:
            if any(fnmatch.fnmatchcase(p, stem) for p in tracked):
                resolved += 1
            else:
                unresolved.append(token)
        elif stem in tracked_set or stem in directories:
            resolved += 1
        else:
            unresolved.append(token)

    for token in unresolved:
        print("UNRESOLVED %s lines=%s" % (token, ",".join(str(n) for n in sorted(found[token]))))
    if "--list" in flags:
        for token in listed:
            print("BY-DESIGN %s" % token)
    print(
        "paths: candidates=%d resolved=%d abbreviated=%d abbreviated_unique=%d "
        "placeholders=%d by_design=%d unresolved=%d"
        % (
            len(found),
            resolved,
            abbreviated,
            abbreviated_unique,
            placeholders,
            by_design,
            len(unresolved),
        )
    )
    return 0 if not unresolved else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
