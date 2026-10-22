package com.clickhouse.kafka.connect.sink.db.helper;

import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.JSONL_SUFFIX;
import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.describeLine;
import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.resourceLines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.data.ClickHouseDataType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free unit table of {@link DescribeTableBuilder} over the real DESCRIBE rows committed for
 * the 47 fixture tables (DESC-08, D-05). The DDL of each fixture is the oracle: no list of expected
 * names is written by hand. Deliberately extends nothing, so the class stays in {@code testUnit}.
 */
class DescribeTableBuilderUnitTest {

    private static final String DATABASE = "db";
    private static final String TABLE = "t";
    private static final String WARN_LEVEL = "WARN";
    private static final String NULL_SUFFIX = ".null";
    private static final String KEYS_SUFFIX = ".keys";
    private static final String NAME_SEPARATOR = ".";

    /** A Tuple type that {@code ClickHouseColumn.of} rejects (premise-checked) and that holds the names size and size0. */
    private static final String UNREADABLE_TUPLE_TYPE = "Tuple(size UInt8, size0 UInt8, v String";

    static Stream<String> fixtureKeys() {
        return DescribeFixtureCatalog.FIXTURES.stream().map(DescribeFixtureCatalog.Fixture::key);
    }

    private static Table build(String key) throws JsonProcessingException {
        DescribeTableBuilder builder = new DescribeTableBuilder(DATABASE, TABLE);
        for (String line : resourceLines(key + JSONL_SUFFIX)) {
            assertTrue(builder.addRow(line), key + ": every committed row is a column the connector can handle");
        }
        return builder.build();
    }

    private static Table buildRows(String... nameAndTypePairs) throws JsonProcessingException {
        DescribeTableBuilder builder = new DescribeTableBuilder(DATABASE, TABLE);
        for (int i = 0; i < nameAndTypePairs.length; i += 2) {
            assertTrue(
                    builder.addRow(describeLine(nameAndTypePairs[i], nameAndTypePairs[i + 1])),
                    nameAndTypePairs[i] + " is a column the connector can handle");
        }
        return builder.build();
    }

    private static void visit(Column column, Consumer<Column> visitor) {
        if (column == null) {
            return;
        }
        visitor.accept(column);
        visit(column.getArrayType(), visitor);
        visit(column.getMapValueType(), visitor);
        if (column.getTupleFields() != null) {
            column.getTupleFields().forEach(field -> visit(field, visitor));
        }
    }

    private static List<String> tupleFieldNamesAtAnyDepth(Table table) {
        List<String> names = new ArrayList<>();
        for (Column root : table.getRootColumnsList()) {
            visit(root, node -> {
                if (node.getTupleFields() != null) {
                    node.getTupleFields().forEach(field -> names.add(field.getName()));
                }
            });
        }
        return names;
    }

    private static ClickHouseColumn unwrapSimpleAggregate(ClickHouseColumn parsed) {
        ClickHouseColumn node = parsed;
        while (node.getDataType() == ClickHouseDataType.SimpleAggregateFunction
                && !node.getNestedColumns().isEmpty()) {
            node = node.getNestedColumns().get(0);
        }
        return node;
    }

    private static void assertTupleFieldsEqualDeclaredElements(String key, Column built, ClickHouseColumn declared) {
        if (built == null) {
            return;
        }
        ClickHouseColumn parsed = unwrapSimpleAggregate(declared);
        switch (built.getType()) {
            case ARRAY:
                assertTupleFieldsEqualDeclaredElements(
                        key, built.getArrayType(), parsed.getNestedColumns().get(0));
                return;
            case MAP:
                assertTupleFieldsEqualDeclaredElements(
                        key, built.getMapValueType(), parsed.getNestedColumns().get(1));
                return;
            case TUPLE:
                List<ClickHouseColumn> elements = parsed.getNestedColumns();
                List<String> expected = new ArrayList<>();
                for (int i = 0; i < elements.size(); i++) {
                    expected.add(DeclaredNullability.elementKey(elements.get(i), i));
                }
                String prefix = built.getName() + NAME_SEPARATOR;
                List<String> actual = new ArrayList<>();
                for (Column field : built.getTupleFields()) {
                    assertTrue(field.getName().startsWith(prefix), key + ": " + field.getName() + " under " + built.getName());
                    actual.add(field.getName().substring(prefix.length()));
                }
                assertEquals(expected, actual, key + ": tupleFields of " + built.getName() + " must equal the declared elements in order");
                for (Column field : built.getTupleFields()) {
                    String fieldKey = field.getName().substring(prefix.length());
                    int index = expected.indexOf(fieldKey);
                    assertTrue(index >= 0, key + ": " + fieldKey);
                    assertTupleFieldsEqualDeclaredElements(key, field, elements.get(index));
                }
                return;
            default:
                return;
        }
    }

    @ParameterizedTest(name = "DESC-08 declared elements: {0}")
    @MethodSource("fixtureKeys")
    @DisplayName("DESC-08 declared elements: the tupleFields of every Tuple equal the declared element keys in declared order")
    void tupleFieldsEqualTheDeclaredElements(String key) throws Exception {
        Table table = build(key);
        for (String line : resourceLines(key + JSONL_SUFFIX)) {
            ClickHouseFieldDescriptor row = ClickHouseFieldDescriptor.fromJsonRow(line);
            if (row.isSubcolumn()) {
                continue;
            }
            Column root = table.getRootColumnsMap().get(row.getName());
            assertNotNull(root, key + ": root " + row.getName());
            if (!DeclaredNullability.containsTuple(root)
                    || root.getType() == Type.JSON
                    || root.getType() == Type.DYNAMIC) {
                continue;
            }
            ClickHouseColumn declared =
                    ClickHouseColumn.of(row.getName(), DeclaredNullability.normalise(row.getType()));
            assertTupleFieldsEqualDeclaredElements(key, root, declared);
        }
    }

    @ParameterizedTest(name = "D-10 no WARN: {0}")
    @MethodSource("fixtureKeys")
    @DisplayName("D-10 no WARN: building a fixture logs no WARN on the discovery or the nullability logger")
    void buildingAFixtureLogsNoWarning(String key) throws Exception {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            build(key);

            assertEquals(List.of(), helperLog.events(WARN_LEVEL).stream().map(LogCapture.CapturedEvent::message).toList(), key);
            assertEquals(List.of(), nullabilityLog.events(WARN_LEVEL).stream().map(LogCapture.CapturedEvent::message).toList(), key);
        }
    }

    @Test
    @DisplayName("DESC-08 cascade guard: the children of a declared size-named element are Columns")
    void childrenOfADeclaredSizeNamedElementAreColumns() throws Exception {
        Column n02Element = build("n02").getRootColumnsMap().get("c").getArrayType();
        assertEquals(List.of("c.size0.a"), fieldNamesOf(fieldNamed(n02Element, "c.size0")), "n02");

        Column x01Root = build("x01").getRootColumnsMap().get("c");
        assertEquals(List.of("c.size.a", "c.size.b"), fieldNamesOf(fieldNamed(x01Root, "c.size")), "x01");

        Column x02Element = build("x02").getRootColumnsMap().get("c").getArrayType();
        assertEquals(List.of("c.size.a"), fieldNamesOf(fieldNamed(x02Element, "c.size")), "x02");
    }

    private static Column fieldNamed(Column tuple, String name) {
        assertEquals(Type.TUPLE, tuple.getType(), tuple.getName());
        Column field = tuple.getTupleFields().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " is not a field of " + tuple.getName()));
        assertEquals(Type.TUPLE, field.getType(), name + " is a Tuple");
        return field;
    }

    private static List<String> fieldNamesOf(Column tuple) {
        return tuple.getTupleFields().stream().map(Column::getName).toList();
    }

    @Test
    @DisplayName("DESC-08 cascade guard: a structural row below a declared element is still dropped")
    void aStructuralRowBelowADeclaredElementIsStillDropped() throws Exception {
        Table n03 = build("n03");
        assertFalse(n03.getAllColumnsMap().containsKey("c.size0.size"), "n03 c.size0.size");
        assertFalse(n03.getAllColumnsMap().containsKey("c.size.size"), "n03 c.size.size");
        assertFalse(build("x03").getAllColumnsMap().containsKey("size.size0.size"), "x03 size.size0.size");
    }

    @Test
    @DisplayName("DESC-08 structural rows: no null map is a Column and no Map keys row is a Tuple field")
    void noNullMapIsAColumnAndNoKeysRowIsATupleField() throws Exception {
        for (String key : DescribeFixtureCatalog.FIXTURES.stream().map(DescribeFixtureCatalog.Fixture::key).toList()) {
            for (Column column : build(key).getAllColumnsList()) {
                assertFalse(column.getName().endsWith(NULL_SUFFIX), key + ": " + column.getName());
            }
        }
        for (String key : List.of("s4_a05", "s4_u03", "n04", "x07", "s4_o01")) {
            for (String name : tupleFieldNamesAtAnyDepth(build(key))) {
                assertFalse(name.endsWith(KEYS_SUFFIX), key + ": " + name);
            }
        }
    }

    @Test
    @DisplayName("DESC-08 structural rows: nested size1 offsets rows stay registered and out of every Tuple")
    void nestedSizeOneOffsetsRowsStayRegisteredAndOutOfEveryTuple() throws Exception {
        assertRegisteredOnceAndInNoTuple(build("s4_o01"), "c.size1");
        assertRegisteredOnceAndInNoTuple(build("x07"), "c.values.size1");
    }

    private static void assertRegisteredOnceAndInNoTuple(Table table, String name) {
        assertEquals(
                1,
                table.getAllColumnsList().stream().filter(c -> c.getName().equals(name)).count(),
                name + " is registered exactly once");
        assertFalse(tupleFieldNamesAtAnyDepth(table).contains(name), name + " is in no Tuple");
    }

    @Test
    @DisplayName("DESC-08: a structural size1 row and a declared size1 element register the declared one in either row order")
    void theDeclaredElementWinsTheSharedNameInEitherRowOrder() throws Exception {
        String root = "Array(Array(Tuple(size1 Tuple(a UInt8), v String)))";
        String[] structuralFirst = {
            "c", root, "c.size0", "UInt64", "c.size1", "Array(UInt64)",
            "c.size1", "Array(Array(Tuple(a UInt8)))", "c.size1.a", "Array(Array(UInt8))", "c.v", "Array(Array(String))"
        };
        String[] declaredFirst = {
            "c", root, "c.size0", "UInt64", "c.size1", "Array(Array(Tuple(a UInt8)))",
            "c.size1", "Array(UInt64)", "c.size1.a", "Array(Array(UInt8))", "c.v", "Array(Array(String))"
        };

        Table first = buildRows(structuralFirst);
        Table second = buildRows(declaredFirst);

        for (Table table : List.of(first, second)) {
            assertTrue(table.getAllColumnsMap().get("c.size1").isDeclaredElement(), "c.size1 is the declared element");
            assertEquals(Type.ARRAY, table.getAllColumnsMap().get("c.size1").getType());
            assertTrue(tupleFieldNamesAtAnyDepth(table).contains("c.size1"), "c.size1 is a field of the Tuple");
        }
        assertEquals(tupleFieldNamesAtAnyDepth(first), tupleFieldNamesAtAnyDepth(second));
        assertEquals(
                first.getAllColumnsMap().get("c.size1").getArrayType().getArrayType().getTupleFields().stream()
                        .map(Column::getName)
                        .toList(),
                second.getAllColumnsMap().get("c.size1").getArrayType().getArrayType().getTupleFields().stream()
                        .map(Column::getName)
                        .toList(),
                "the declared element keeps its own children");
    }

    @Test
    @DisplayName("DESC-05: no Column below a JSON root")
    void noColumnBelowAJsonRoot() throws Exception {
        assertEquals(
                List.of("id", "j"),
                build("x08").getAllColumnsList().stream().map(Column::getName).toList());
    }

    @Test
    @DisplayName("D-03, D-04: an unreadable parent keeps the earlier behaviour (the size-named element is dropped,"
            + " the table is still served) with one metadata-only WARN")
    void anUnreadableParentKeepsTheEarlierBehaviourWithOneMetadataOnlyWarning() throws Exception {
        assertThrows(
                RuntimeException.class,
                () -> ClickHouseColumn.of("c.x", DeclaredNullability.normalise(UNREADABLE_TUPLE_TYPE)),
                "premise: the parser rejects the unreadable type");

        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Table table = buildRows(
                    "c", "Tuple(x Tuple(size UInt8, size0 UInt8, v String))",
                    "c.x", UNREADABLE_TUPLE_TYPE,
                    "c.x.size", "UInt8",
                    "c.x.size0", "UInt8",
                    "c.x.v", "String",
                    "c.x.v.size", "UInt64");

            Column unreadable = table.getAllColumnsMap().get("c.x");
            // Not fail-closed on purpose (D-03 rejected refusing the table): the declared size and size0 elements
            // are still dropped, so the Tuple stays short exactly as in earlier releases and the WARNs are the signal.
            assertEquals(List.of("c.x.v"), fieldNamesOf(unreadable));

            List<LogCapture.CapturedEvent> helperWarnings = helperLog.events(WARN_LEVEL);
            assertEquals(1, helperWarnings.size());
            String message = helperWarnings.get(0).message();
            assertTrue(message.contains(DATABASE + NAME_SEPARATOR + TABLE), message);
            assertTrue(message.contains("c.x"), message);
            assertTrue(message.contains(UNREADABLE_TUPLE_TYPE), message);
            assertFalse(helperWarnings.get(0).hasThrowable());

            List<LogCapture.CapturedEvent> nullabilityWarnings = nullabilityLog.events(WARN_LEVEL);
            assertEquals(1, nullabilityWarnings.size());
            assertTrue(nullabilityWarnings.get(0).message().contains("[c.x.size, c.x.size0]"), nullabilityWarnings.get(0).message());
        }
    }

    private static final String JSON_ROOT = "j";
    private static final String NO_PARENT_TYPE = null;

    static Stream<Arguments> ruleRows() {
        return Stream.of(
                rule("root row", "size", "UInt8", null, NO_PARENT_TYPE, false, DescribeTableBuilder.RowDecision.KEEP),
                rule("null map", "c.null", "UInt8", "c", "Nullable(Tuple(size UInt8, v String))", false,
                        DescribeTableBuilder.RowDecision.SKIP_STRUCTURAL),
                rule("below a JSON root", "j.a.size", "UInt8", "j.a", "Tuple(size UInt8, v String)", true,
                        DescribeTableBuilder.RowDecision.SKIP_STRUCTURAL),
                rule("not size-named", "c.sizes", "UInt8", "c", "Tuple(sizes UInt8, v String)", false,
                        DescribeTableBuilder.RowDecision.KEEP),
                rule("no raw type, size0", "c.size0", "UInt64", "c", NO_PARENT_TYPE, false,
                        DescribeTableBuilder.RowDecision.SKIP_UNDECIDABLE),
                rule("no raw type, size1", "c.size1", "Array(UInt64)", "c", NO_PARENT_TYPE, false,
                        DescribeTableBuilder.RowDecision.KEEP_UNDECIDABLE),
                rule("unreadable parent", "c.size", "UInt8", "c", UNREADABLE_TUPLE_TYPE, false,
                        DescribeTableBuilder.RowDecision.SKIP_UNDECIDABLE),
                rule("String length row", "c.v.size", "UInt64", "c.v", "String", false,
                        DescribeTableBuilder.RowDecision.SKIP_STRUCTURAL_SIZE),
                rule("Map offsets row", "c.size0", "UInt64", "c", "Map(String, Tuple(size UInt8, v String))", false,
                        DescribeTableBuilder.RowDecision.SKIP_STRUCTURAL_SIZE),
                rule("Array offsets twin", "c.size0", "UInt64", "c", "Array(Tuple(size0 UInt8, v String))", false,
                        DescribeTableBuilder.RowDecision.SKIP_STRUCTURAL_SIZE),
                rule("declared size0 in an Array", "c.size0", "Array(UInt8)", "c", "Array(Tuple(size0 UInt8, v String))", false,
                        DescribeTableBuilder.RowDecision.KEEP_DECLARED),
                rule("declared bare size", "c.size", "UInt16", "c", "Tuple(size UInt16, v String)", false,
                        DescribeTableBuilder.RowDecision.KEEP_DECLARED),
                rule("inner offsets at depth 2", "c.size1", "Array(UInt64)", "c",
                        "Array(Array(Tuple(size1 UInt8, size0 UInt8)))", false,
                        DescribeTableBuilder.RowDecision.KEEP_STRUCTURAL_SIZE),
                rule("declared size1 with the offsets type at depth 1", "c.size1", "Array(UInt64)", "c",
                        "Array(Tuple(size1 UInt64, v String))", false, DescribeTableBuilder.RowDecision.KEEP_DECLARED),
                rule("declared size1 outside an Array", "c.size1", "UInt8", "c", "Tuple(size1 UInt8, v String)", false,
                        DescribeTableBuilder.RowDecision.KEEP_DECLARED),
                rule("size1 under an Array(Map) parent", "c.size1", "Array(UInt64)", "c",
                        "Array(Map(String, Tuple(size1 UInt8)))", false,
                        DescribeTableBuilder.RowDecision.KEEP_STRUCTURAL_SIZE),
                rule("leading-zero suffix", "c.size01", "Array(UInt64)", "c", "Array(Tuple(size01 UInt64))", false,
                        DescribeTableBuilder.RowDecision.KEEP_DECLARED));
    }

    private static Arguments rule(
            String label,
            String name,
            String type,
            String parent,
            String parentType,
            boolean jsonAncestor,
            DescribeTableBuilder.RowDecision expected) {
        return Arguments.of(label, name, type, parent, parentType, jsonAncestor, expected);
    }

    @ParameterizedTest(name = "DESC-08 rule: {0}")
    @MethodSource("ruleRows")
    @DisplayName("DESC-08 rule: {0}")
    void theRuleDecidesEachRow(
            String label,
            String name,
            String type,
            String parent,
            String parentType,
            boolean jsonAncestor,
            DescribeTableBuilder.RowDecision expected)
            throws Exception {
        Map<String, Column> columnsSoFar = new HashMap<>();
        if (jsonAncestor) {
            columnsSoFar.put(JSON_ROOT, Column.extractColumn(JSON_ROOT, "JSON", false, false, false));
        }
        Map<String, String> rawTypes = new HashMap<>();
        if (parentType != null) {
            rawTypes.put(parent, parentType);
        }
        ClickHouseFieldDescriptor row = ClickHouseFieldDescriptor.fromJsonRow(describeLine(name, type));

        assertEquals(expected, DescribeTableBuilder.decide(row, columnsSoFar, rawTypes), label);
        assertEquals(expected.skipsRow(), ClickHouseHelperClient.shouldSkipDescribeRow(row, columnsSoFar, rawTypes), label);
    }

    @Test
    @DisplayName("DESC-08 carrier: a declared size-named element carries declaredElement and is the registered Column of its name")
    void aDeclaredElementCarriesTheFlagAndIsTheRegisteredColumn() throws Exception {
        Map<String, Column> n01 = build("n01").getAllColumnsMap();
        assertTrue(n01.get("c.size1").isDeclaredElement(), "n01 c.size1");
        assertTrue(n01.get("c.size0").isDeclaredElement(), "n01 c.size0");
        assertTrue(build("x04").getAllColumnsMap().get("c.size1").isDeclaredElement(), "x04 c.size1");

        Map<String, Column> o01 = build("s4_o01").getAllColumnsMap();
        assertFalse(o01.get("c.size1").isDeclaredElement(), "s4_o01 c.size1 is the nested offsets row");
        assertTrue(o01.get("c.values.size1").isDeclaredElement(), "s4_o01 c.values.size1");
    }

    @Test
    @DisplayName("Table.updateParent: an Array parent adds a flagged size-named child to its Tuple and still ignores an unflagged one")
    void anArrayParentAddsAFlaggedSizeNamedChildAndIgnoresAnUnflaggedOne() {
        assertEquals(List.of("e.size1", "e.v"), elementTupleFieldNames(true));
        assertEquals(List.of("e.v"), elementTupleFieldNames(false));
    }

    private static List<String> elementTupleFieldNames(boolean flagged) {
        Column parent = Column.extractColumn("e", "Array(Tuple(size1 UInt64, v String))", false, false, false);
        Column sizeChild = Column.extractColumn("e.size1", "Array(UInt64)", false, false, true);
        sizeChild.setDeclaredElement(flagged);
        Column valueChild = Column.extractColumn("e.v", "Array(String)", false, false, true);
        Table table = new Table(DATABASE, TABLE, false, List.of(parent, sizeChild, valueChild), 1);
        return fieldNamesOf(table.getRootColumnsMap().get("e").getArrayType());
    }

    @Test
    @DisplayName("Table.updateParent: the Map branch still ignores a size-named child")
    void theMapBranchStillIgnoresASizeNamedChild() {
        Column parent = Column.extractColumn("m", "Map(String, UInt8)", false, false, false);
        Column sizeChild = Column.extractColumn("m.size0", "UInt64", false, false, true);
        sizeChild.setDeclaredElement(true);

        Table table = new Table(DATABASE, TABLE, false, List.of(parent, sizeChild), 1);

        assertEquals(List.of("m"), table.getRootColumnsList().stream().map(Column::getName).toList());
        assertEquals(Type.MAP, table.getRootColumnsMap().get("m").getType());
    }
}
