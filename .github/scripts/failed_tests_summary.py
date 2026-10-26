#!/usr/bin/env python3
"""Render the Gradle JUnit XML reports of the integrationTest task into a
Markdown summary on stdout, appended to $GITHUB_STEP_SUMMARY when that is set,
plus one workflow-command annotation line. Job logs are not readable without
repository admin rights, so the failed test names must reach the step summary
and an annotation.

Fork addition for the nightly integration workflow (Phase 31, D-10). Standard
library only: xml.etree.ElementTree needs no third-party dependency.

Usage: failed_tests_summary.py <directory holding TEST-*.xml>
Exit status: 0 after a valid argument (a missing directory or no report is a
warning, not an error), 2 on a usage error.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

SUMMARY_HEADING = "### Integration tests (ClickHouse latest, client V2)"
ANNOTATION_CAP = 1000
ERROR_TITLE = "Integration tests failed"
NOTICE_TITLE = "Integration tests"
WARNING_TITLE = "Integration test reports"
SUMMARY_ENV = "GITHUB_STEP_SUMMARY"
REPORT_PATTERN = "TEST-*.xml"
MESSAGE_CAP = 200
# A single name longer than this is cut, so the first name always fits ANNOTATION_CAP once
# percent-escaped (every character grows to at most three).
ANNOTATION_NAME_CAP = 300
USAGE = "usage: failed_tests_summary.py <directory holding TEST-*.xml>"


def escape_command(text):
    """Escape text for the data part of a workflow command (percent first)."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_markdown(text):
    """Make text safe inside one Markdown table cell."""
    return (
        text.replace("|", "\\|")
        .replace("`", "'")
        .replace("\r", " ")
        .replace("\n", " ")
    )


def first_line(text):
    """The first non-blank line of text, cut to MESSAGE_CAP characters."""
    for line in (text or "").splitlines():
        if line.strip():
            return line.strip()[:MESSAGE_CAP]
    return ""


def collect(directory):
    """Parse every TEST-*.xml of directory, in sorted order.

    Returns (totals, failed, unreadable): totals counts tests, failures, errors
    and skipped testcases; failed lists (classname#name, kind, first message
    line) for each failed or errored testcase; unreadable lists the names of the
    files that did not parse, which are skipped instead of failing the run.
    """
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed = []
    unreadable = []
    for path in sorted(glob.glob(os.path.join(directory, REPORT_PATTERN))):
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            unreadable.append(os.path.basename(path))
            continue
        for case in root.iter("testcase"):
            totals["tests"] += 1
            failure = case.find("failure")
            error = case.find("error")
            if case.find("skipped") is not None:
                totals["skipped"] += 1
            if failure is not None:
                totals["failures"] += 1
            if error is not None:
                totals["errors"] += 1
            problem = failure if failure is not None else error
            if problem is None:
                continue
            kind = "failure" if failure is not None else "error"
            name = "{}#{}".format(case.get("classname", ""), case.get("name", ""))
            message = problem.get("message") or problem.text
            failed.append((name, kind, first_line(message)))
    return totals, failed, unreadable


def failed_annotation(names):
    """One ::error line naming the failed tests, capped at ANNOTATION_CAP characters."""
    prefix = "::error title={}::{} failed: ".format(ERROR_TITLE, len(names))
    escaped = [escape_command(name[:ANNOTATION_NAME_CAP]) for name in names]
    joined = ", ".join(escaped)
    if len(joined) <= ANNOTATION_CAP:
        return prefix + joined
    kept = []
    used = 0
    for item in escaped:
        added = len(item) + (2 if kept else 0)
        if used + added > ANNOTATION_CAP:
            break
        kept.append(item)
        used += added
    return prefix + ", ".join(kept) + " ... (+{} more)".format(len(escaped) - len(kept))


def render(directory):
    """Return (markdown lines, annotation lines) for the reports under directory."""
    reports = glob.glob(os.path.join(directory, REPORT_PATTERN))
    if not os.path.isdir(directory) or not reports:
        markdown = [
            SUMMARY_HEADING,
            "",
            "No JUnit XML reports found under {}; nothing to summarise.".format(directory),
        ]
        annotations = [
            "::warning title={}::no JUnit XML reports under {}".format(
                WARNING_TITLE, escape_command(directory)
            )
        ]
        return markdown, annotations

    totals, failed, unreadable = collect(directory)
    markdown = [
        SUMMARY_HEADING,
        "",
        "tests={tests} failures={failures} errors={errors} skipped={skipped}".format(**totals),
        "",
    ]
    if failed:
        markdown.append("| Test | Kind | First message line |")
        markdown.append("|---|---|---|")
        for name, kind, message in failed:
            markdown.append(
                "| {} | {} | {} |".format(
                    escape_markdown(name), kind, escape_markdown(message)
                )
            )
    else:
        markdown.append("No failed tests.")
    annotations = []
    if unreadable:
        markdown.extend(["", "Unreadable report files: {}".format(", ".join(unreadable))])
        annotations.append(
            "::warning title={}::unreadable report files: {}".format(
                WARNING_TITLE, escape_command(", ".join(unreadable))
            )
        )
    if failed:
        annotations.append(failed_annotation([name for name, _, _ in failed]))
    else:
        annotations.append(
            "::notice title={}::tests={} failures=0 errors=0 skipped={}".format(
                NOTICE_TITLE, totals["tests"], totals["skipped"]
            )
        )
    return markdown, annotations


def main(argv):
    if len(argv) != 2:
        print(USAGE, file=sys.stderr)
        return 2
    sys.stdout.reconfigure(encoding="utf-8")
    markdown, annotations = render(argv[1])
    for line in markdown + annotations:
        print(line)
    summary_path = os.environ.get(SUMMARY_ENV)
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write("\n".join(markdown) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
