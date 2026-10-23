#!/usr/bin/env python3
"""Render the two JaCoCo XML reports this build produces into one markdown
coverage summary, written to both stdout and $GITHUB_STEP_SUMMARY (if set).

Fork modification for Phase 5 (CI & Final Verification) - see
.planning/REQUIREMENTS.md CI-03. Standard library only, no third-party
dependency: xml.etree.ElementTree is non-validating and ignores the JaCoCo
XML's external DTD reference without any network access.

Inputs (relative to the repository root, produced by build.gradle.kts's
jacocoTestReport and jacocoForkReport tasks, both finalizedBy(test)):
  - build/reports/jacoco/test/jacocoTestReport.xml  (full suite)
  - build/reports/jacoco/fork/jacocoForkReport.xml  (fork-changed classes)
"""
import os
import sys
import xml.etree.ElementTree as ET

TEST_REPORT_PATH = "build/reports/jacoco/test/jacocoTestReport.xml"
FORK_REPORT_PATH = "build/reports/jacoco/fork/jacocoForkReport.xml"

# The 21 fork-changed base class names from build.gradle.kts's
# forkChangedClasses list, in declaration order, with the .class suffix
# stripped and the inner-class wildcard entries (X$*.class) dropped - those
# are JaCoCo include globs, not separate classes to tabulate.
FORK_CLASSES_IN_ORDER = [
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter",
    "com/clickhouse/kafka/connect/transforms/HeadersToValue",
    "com/clickhouse/kafka/connect/transforms/HeaderToValue",
    "com/clickhouse/kafka/connect/transforms/HeaderToValueConfig",
    "com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient",
    "com/clickhouse/kafka/connect/sink/db/mapping/Column",
    "com/clickhouse/kafka/connect/sink/db/mapping/Type",
    "com/clickhouse/kafka/connect/sink/db/mapping/Table",
    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig",
    "com/clickhouse/kafka/connect/sink/ProxySinkTask",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask",
    "com/clickhouse/kafka/connect/ClickHouseSinkConnector",
    "com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor",
    "com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder",
    "com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability",
    "com/clickhouse/kafka/connect/sink/db/mapping/ParentShape",
    "com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter",
    "com/clickhouse/kafka/connect/util/WithheldMessageException",
]


def pct(counter):
    """Render a JaCoCo <counter> element as a percentage string.

    JaCoCo omits a counter type entirely when a class has zero instructions
    of that kind (verified live against this repo's real fork report:
    HeaderToValueConfig and Type have no BRANCH counter) - that case renders
    the literal "n/a", never a crash and never a misleading "0.0%". A
    present counter whose missed+covered total is zero renders "0.0%".
    """
    if counter is None:
        return "n/a"
    missed = int(counter.get("missed"))
    covered = int(counter.get("covered"))
    total = missed + covered
    if total == 0:
        return "0.0%"
    return "{:.1f}%".format(100 * covered / total)


def report_totals(xml_path):
    """Report-level totals: counter elements that are DIRECT children of
    the <report> root element, indexed by their type attribute."""
    root = ET.parse(xml_path).getroot()
    return {c.get("type"): c for c in root.findall("counter")}


def class_counters(xml_path):
    """Class-level totals: for each <class> element, the counter elements
    that are DIRECT children of it (a non-recursive findall, never a
    recursive search - the counter tag name is identical at method, class,
    package and report level)."""
    root = ET.parse(xml_path).getroot()
    result = {}
    for cls in root.iter("class"):
        result[cls.get("name")] = {c.get("type"): c for c in cls.findall("counter")}
    return result


def render_markdown():
    for path in (TEST_REPORT_PATH, FORK_REPORT_PATH):
        if not os.path.isfile(path):
            sys.exit("jacoco_summary: missing JaCoCo report: " + path)

    full = report_totals(TEST_REPORT_PATH)
    fork = report_totals(FORK_REPORT_PATH)
    fork_classes = class_counters(FORK_REPORT_PATH)

    lines = [
        "## Coverage Summary",
        "",
        "| Scope | Instruction | Line | Branch |",
        "|---|---|---|---|",
        "| Full suite | {} | {} | {} |".format(
            pct(full.get("INSTRUCTION")), pct(full.get("LINE")), pct(full.get("BRANCH"))
        ),
        "| Fork-changed classes | {} | {} | {} |".format(
            pct(fork.get("INSTRUCTION")), pct(fork.get("LINE")), pct(fork.get("BRANCH"))
        ),
        "",
        "### Fork-changed classes",
        "",
        "| Class | Line | Branch |",
        "|---|---|---|",
    ]
    for name in FORK_CLASSES_IN_ORDER:
        counters = fork_classes.get(name, {})
        short = name.rsplit("/", 1)[-1]
        lines.append("| {} | {} | {} |".format(short, pct(counters.get("LINE")), pct(counters.get("BRANCH"))))

    return "\n".join(lines)


def main():
    markdown = render_markdown()
    print(markdown)

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fh:
            fh.write(markdown + "\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
