#!/usr/bin/env python3
"""Derive and check the Kafka API the connector references (BLD-16, D-11, D-12, D-13).

Fork modification for Phase 25. Standard library plus the JDK's javap.

The connector compiles against connect-api 4.4.0, but production loads it on the
Kafka Connect worker image (Kafka 3.9), which supplies the Connect API at runtime.
A reference to a class or member that exists at build time but not on the worker
fails at class-load time, not at build time. This script closes that gap:

  derive   (D-11) read the constant pool of every compiled connector class with
           `javap -v -p` and collect every Class, Fieldref, Methodref and
           InterfaceMethodref entry under org/apache/kafka. References whose owner is
           a connector class are resolved through the superclass chain and then the
           interfaces, so a member inherited from a Kafka type (for example
           AbstractConfig.getBoolean called on ClickHouseSinkConfig) is attributed to
           the Kafka type that declares it.
  check    (D-12) for each reference label (the worker jars, 4.1.1, 4.4.0) prove that
           every class entry is a class file in the jars and every member entry is
           declared by the owner or one of its supertypes in those jars.
  write    (D-13) store the blessed set as src/test/resources/connect-api-references.txt,
           only when every reference reports 0 missing; the file is replaced atomically.

Entry format (sorted, one per line):
  C org/apache/kafka/connect/sink/SinkTask
  M org/apache/kafka/connect/sink/SinkConnector.validate:(Ljava/util/Map;)Lorg/apache/kafka/common/config/Config;

Usage:
  python3 -I connect-api-refs.py --classes DIR --compile-classpath JARS \\
      --reference LABEL=JARS [--reference LABEL=JARS ...] [--write FILE] [--image-note TEXT ...]

JARS is a colon-separated list of jar paths. Exit status 0 only when every reference
reports 0 missing classes and 0 missing members.
"""
import argparse
import datetime
import hashlib
import os
import re
import subprocess
import sys
import tempfile
import zipfile

KAFKA_PREFIX = "org/apache/kafka/"
OWN_PREFIX = "com/clickhouse/kafka/connect/"
CHUNK = 40

POOL_LINE = re.compile(r"^\s+#(\d+) = (\S+)\s+(.*)$")
REF_OPERANDS = re.compile(r"#(\d+)\.#(\d+)")
NAT_OPERANDS = re.compile(r"#(\d+):#(\d+)")
INDEX_OPERAND = re.compile(r"#(\d+)")


def run_javap(args):
    """Run javap; return stdout. Missing classes make javap print an error and return nothing useful."""
    result = subprocess.run(["javap"] + args, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return result.stdout


def parse_constant_pools(javap_output):
    """Yield one list of (index, kind, operands-text) per class in a `javap -v` run."""
    pools = []
    current = None
    in_pool = False
    for line in javap_output.splitlines():
        if line.startswith("Classfile "):
            current = {}
            pools.append(current)
            in_pool = False
            continue
        if current is None:
            continue
        if line.startswith("Constant pool:"):
            in_pool = True
            continue
        if in_pool and line == "{":
            in_pool = False
            continue
        if in_pool:
            m = POOL_LINE.match(line)
            if m:
                current[int(m.group(1))] = (m.group(2), m.group(3))
    return pools


def unwrap_class_name(name):
    """Strip array dimensions and the L...; wrapper from a Class entry's name."""
    stripped = name.lstrip("[")
    if stripped != name and stripped.startswith("L") and stripped.endswith(";"):
        stripped = stripped[1:-1]
    return stripped


def entries_of_pool(pool):
    """Return (class names, member refs as (owner, name, descriptor)) of one constant pool."""

    def utf8(index):
        kind, rest = pool[index]
        if kind != "Utf8":
            raise ValueError("constant #%d is %s, expected Utf8" % (index, kind))
        return rest

    def class_name(index):
        kind, rest = pool[index]
        if kind != "Class":
            raise ValueError("constant #%d is %s, expected Class" % (index, kind))
        return unwrap_class_name(utf8(int(INDEX_OPERAND.match(rest).group(1))))

    classes = set()
    members = set()
    for kind, rest in pool.values():
        if kind == "Class":
            classes.add(unwrap_class_name(utf8(int(INDEX_OPERAND.match(rest).group(1)))))
    for kind, rest in pool.values():
        if kind in ("Fieldref", "Methodref", "InterfaceMethodref"):
            class_index, nat_index = (int(g) for g in REF_OPERANDS.match(rest).groups())
            name_index, desc_index = (int(g) for g in NAT_OPERANDS.match(pool[nat_index][1]).groups())
            members.add((class_name(class_index), utf8(name_index), utf8(desc_index)))
    return classes, members


def strip_generics(text):
    """Remove balanced <...> groups."""
    depth = 0
    out = []
    for ch in text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth = max(depth - 1, 0)
        elif depth == 0:
            out.append(ch)
    return "".join(out)


class TypeInfo:
    def __init__(self, name, superclass, interfaces, members):
        self.name = name
        self.superclass = superclass
        self.interfaces = interfaces
        self.members = members


class TypeLoader:
    """Loads type signatures with `javap -p -s -cp CLASSPATH`, one cached call per type."""

    def __init__(self, classpath):
        self.classpath = classpath
        self.cache = {}

    def load(self, internal_name):
        if internal_name not in self.cache:
            output = run_javap(["-p", "-s", "-cp", self.classpath, internal_name.replace("/", ".")])
            self.cache[internal_name] = self.parse(internal_name, output)
        return self.cache[internal_name]

    @staticmethod
    def parse(internal_name, output):
        dotted = internal_name.replace("/", ".")
        lines = output.splitlines()
        header = None
        for line in lines:
            if line.rstrip().endswith("{") and re.search(r"\b(class|interface)\b", line):
                header = strip_generics(line.rstrip()[:-1]).strip()
                break
        if header is None:
            return None
        superclass = None
        interfaces = []
        is_interface = re.search(r"\binterface\b", header) is not None
        for keyword in ("extends", "implements"):
            m = re.search(r"\b%s\s+([^{]*?)(?=\s+(?:extends|implements)\b|$)" % keyword, header)
            if m:
                names = [n.strip().replace(".", "/") for n in m.group(1).split(",") if n.strip()]
                if keyword == "extends" and not is_interface:
                    superclass = names[0]
                else:
                    interfaces.extend(names)
        members = set()
        simple = dotted.rsplit(".", 1)[-1]
        for index, line in enumerate(lines):
            stripped = line.strip()
            if not stripped.startswith("descriptor:"):
                continue
            descriptor = stripped.split(":", 1)[1].strip()
            declaration = lines[index - 1].strip()
            if declaration.startswith("static {"):
                continue
            if "(" in declaration:
                name = strip_generics(declaration).split("(", 1)[0].split()[-1]
                if name in (dotted, simple) and descriptor.endswith(")V"):
                    name = "<init>"
            else:
                name = declaration.rstrip(";").split()[-1]
            members.add((name, descriptor))
        return TypeInfo(internal_name, superclass, interfaces, members)


def declaring_type(loader, owner, name, descriptor, skipped):
    """Return the first type declaring the member: the owner and its superclass chain, then the interfaces."""
    chain = []
    interfaces = []
    seen = set()
    current = owner
    while current is not None and current not in seen:
        seen.add(current)
        info = loader.load(current)
        if info is None:
            if not current.startswith(KAFKA_PREFIX) and skipped is not None:
                skipped.add(current)
            break
        chain.append(info)
        interfaces.extend(info.interfaces)
        current = info.superclass
    for info in chain:
        if (name, descriptor) in info.members:
            return info.name
    queue = list(interfaces)
    while queue:
        current = queue.pop(0)
        if current in seen:
            continue
        seen.add(current)
        info = loader.load(current)
        if info is None:
            if not current.startswith(KAFKA_PREFIX) and skipped is not None:
                skipped.add(current)
            continue
        if (name, descriptor) in info.members:
            return info.name
        queue.extend(info.interfaces)
    return None


def class_files(classes_dir):
    root = os.path.join(classes_dir, *OWN_PREFIX.rstrip("/").split("/"))
    found = []
    for directory, _, names in os.walk(root):
        found.extend(os.path.join(directory, n) for n in names if n.endswith(".class"))
    return sorted(found)


def derive_references(classes_dir, compile_classpath):
    """D-11: return (sorted entries, set of skipped supertypes)."""
    files = class_files(classes_dir)
    if not files:
        raise SystemExit("no class files under %s" % os.path.join(classes_dir, OWN_PREFIX))
    entries = set()
    own_refs = set()
    for start in range(0, len(files), CHUNK):
        output = run_javap(["-v", "-p"] + files[start:start + CHUNK])
        pools = parse_constant_pools(output)
        for pool in pools:
            classes, members = entries_of_pool(pool)
            for name in classes:
                if name.startswith(KAFKA_PREFIX):
                    entries.add("C " + name)
            for owner, name, descriptor in members:
                if owner.startswith(KAFKA_PREFIX):
                    entries.add("M %s.%s:%s" % (owner, name, descriptor))
                elif owner.startswith(OWN_PREFIX):
                    own_refs.add((owner, name, descriptor))
    loader = TypeLoader(classes_dir + os.pathsep + compile_classpath)
    skipped = set()
    for owner, name, descriptor in sorted(own_refs):
        declaring = declaring_type(loader, owner, name, descriptor, skipped)
        if declaring is not None and declaring.startswith(KAFKA_PREFIX):
            entries.add("M %s.%s:%s" % (declaring, name, descriptor))
    return sorted(entries), skipped


def check_reference(label, jars, refs):
    """D-12: return (missing class entries, missing member entries) for one reference label."""
    classpath = jars
    names = set()
    for jar in jars.split(os.pathsep):
        with zipfile.ZipFile(jar) as archive:
            names.update(archive.namelist())
    loader = TypeLoader(classpath)
    missing_classes = []
    missing_members = []
    for entry in refs:
        kind, value = entry.split(" ", 1)
        if kind == "C":
            if value + ".class" not in names:
                missing_classes.append(entry)
        else:
            owner, rest = value.split(".", 1)
            name, descriptor = rest.split(":", 1)
            if declaring_type(loader, owner, name, descriptor, None) is None:
                missing_members.append(entry)
    print("REFERENCE %s classes-missing %d members-missing %d" % (label, len(missing_classes), len(missing_members)))
    for entry in missing_classes + missing_members:
        print("MISSING %s %s" % (label, entry))
    return missing_classes, missing_members


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def write_list(path, refs, header):
    """D-13: write header lines (each prefixed with '# ') then the sorted entries, replacing PATH atomically."""
    directory = os.path.dirname(os.path.abspath(path))
    handle, temp_path = tempfile.mkstemp(dir=directory, prefix=".connect-api-refs-", suffix=".tmp")
    try:
        with os.fdopen(handle, "w", encoding="utf-8", newline="\n") as out:
            for line in header:
                out.write(("# " + line).rstrip() + "\n")
            for entry in sorted(refs):
                out.write(entry + "\n")
        os.replace(temp_path, path)
    except BaseException:
        if os.path.exists(temp_path):
            os.unlink(temp_path)
        raise


def build_header(references, results, image_notes, classes_dir):
    lines = [
        "Kafka API the connector references (BLD-16, D-11, D-13); blessed against the production worker's jars.",
        "Derived from the compiled classes under com/clickhouse/kafka/connect/ by .planning/scripts/connect-api-refs.py.",
        "C <class>: a Class constant. M <owner>.<name>:<descriptor>: a Field/Method/InterfaceMethod reference.",
        "ConnectApiReferencesUnitTest fails when the compiled classes reference an entry that is not listed here.",
    ]
    lines.extend(image_notes)
    for label, jars in references:
        for jar in jars.split(os.pathsep):
            lines.append("reference %s: %s sha256 %s" % (label, os.path.basename(jar), sha256_of(jar)))
    for label, (classes, members) in results.items():
        lines.append("result %s: classes-missing %d members-missing %d" % (label, len(classes), len(members)))
    lines.append("re-bless: compileJava, then python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main "
                 "--compile-classpath <connect-api>:<kafka-clients> --reference worker=<worker connect-api>:<worker kafka-clients> "
                 "--write src/test/resources/connect-api-references.txt")
    lines.append("date: " + datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"))
    return lines


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--classes", required=True, help="compiled main classes directory")
    parser.add_argument("--compile-classpath", required=True, help="colon-separated jars the connector compiled against")
    parser.add_argument("--reference", action="append", default=[], metavar="LABEL=JARS")
    parser.add_argument("--write", metavar="FILE")
    parser.add_argument("--image-note", action="append", default=[], help="provenance line for the list header")
    args = parser.parse_args(argv)
    references = []
    for spec in args.reference:
        if "=" not in spec:
            parser.error("--reference expects LABEL=JARS, got %r" % spec)
        label, jars = spec.split("=", 1)
        references.append((label, jars))
    if not references:
        parser.error("at least one --reference is required")
    refs, skipped = derive_references(args.classes, args.compile_classpath)
    for name in sorted(skipped):
        print("SKIPPED-SUPERTYPE %s" % name)
    print("derived classes %d members %d total %d" % (
        sum(1 for r in refs if r.startswith("C ")), sum(1 for r in refs if r.startswith("M ")), len(refs)))
    results = {}
    for label, jars in references:
        results[label] = check_reference(label, jars, refs)
    clean = all(not classes and not members for classes, members in results.values())
    if args.write and clean:
        write_list(args.write, refs, build_header(references, results, args.image_note, args.classes))
        print("WROTE %s" % args.write)
    elif args.write:
        print("NOT WRITTEN %s: a reference reports missing entries" % args.write)
    return 0 if clean else 1


if __name__ == "__main__":
    sys.exit(main())
